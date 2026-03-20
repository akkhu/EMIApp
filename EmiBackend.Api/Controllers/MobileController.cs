using EmiBackend.Api.Data;
using EmiBackend.Api.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;

namespace EmiBackend.Api.Controllers;

[AllowAnonymous]
[ApiController]
[Route("api/[controller]")]
public class MobileController : ControllerBase
{
    private readonly MongoDbService _db;

    public MobileController(MongoDbService db)
    {
        _db = db;
    }

    [HttpGet("{appId}/{aadhaarNumber}/status")]
    public async Task<IActionResult> GetStatus(string appId, string aadhaarNumber)
    {
        var device = await _db.Devices
            .Find(d => d.AppId == appId && d.AadhaarNumber == aadhaarNumber)
            .FirstOrDefaultAsync();

        if (device == null)
        {
            return NotFound(new { message = "Device not found." });
        }

        return Ok(new
        {
            status = device.LockStatus,
            lastSyncTime = DateTime.UtcNow
        });
    }

    [HttpPost("register")]
    public async Task<IActionResult> RegisterDevice([FromBody] MobileRegisterRequest request)
    {
        if (string.IsNullOrEmpty(request.AppId) || string.IsNullOrEmpty(request.AadhaarNumber))
        {
            return BadRequest("AppId and AadhaarNumber are required.");
        }

        var filter = Builders<Device>.Filter.Where(d => d.AppId == request.AppId && d.AadhaarNumber == request.AadhaarNumber);
        var existingDevice = await _db.Devices.Find(filter).FirstOrDefaultAsync();

        if (existingDevice != null)
        {
            // Update existing record
            var update = Builders<Device>.Update
                .Set(d => d.CustomerName, request.CustomerName)
                .Set(d => d.Dob, request.Dob)
                .Set(d => d.AadhaarNumber, request.AadhaarNumber)
                .Set(d => d.PanNumber, request.PanNumber)
                .Set(d => d.FatherName, request.FatherName)
                .Set(d => d.Village, request.Village)
                .Set(d => d.ClientName, request.ClientName)
                .Set(d => d.LastSyncTime, DateTime.UtcNow);

            await _db.Devices.UpdateOneAsync(filter, update);
            return Ok(new { message = "Device registration updated." });
        }
        else
        {
            // Create new record
            var newDevice = new Device
            {
                AppId = request.AppId,
                Imei = "DEPRECATED", // No longer using Imei
                CustomerName = request.CustomerName,
                Dob = request.Dob,
                AadhaarNumber = request.AadhaarNumber,
                PanNumber = request.PanNumber,
                FatherName = request.FatherName,
                Village = request.Village,
                ClientName = request.ClientName,
                LockStatus = "Unlocked",
                LastSyncTime = DateTime.UtcNow
            };
            
            await _db.Devices.InsertOneAsync(newDevice);
            return Ok(new { message = "Device registered successfully." });
        }
    }
}

public class MobileRegisterRequest
{
    public string AppId { get; set; } = string.Empty;
    public string Imei { get; set; } = string.Empty;
    public string CustomerName { get; set; } = string.Empty;
    public string Dob { get; set; } = string.Empty;
    public string AadhaarNumber { get; set; } = string.Empty;
    public string PanNumber { get; set; } = string.Empty;
    public string FatherName { get; set; } = string.Empty;
    public string Village { get; set; } = string.Empty;
    public string ClientName { get; set; } = string.Empty;
}
