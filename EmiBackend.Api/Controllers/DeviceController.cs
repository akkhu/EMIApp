using EmiBackend.Api.Data;
using EmiBackend.Api.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;

namespace EmiBackend.Api.Controllers;

[Authorize]
[ApiController]
[Route("api/[controller]")]
public class DeviceController : ControllerBase
{
    private readonly MongoDbService _db;

    public DeviceController(MongoDbService db)
    {
        _db = db;
    }

    [HttpGet]
    public async Task<IActionResult> GetDevices()
    {
        var devices = await _db.Devices.Find(_ => true).ToListAsync();
        return Ok(devices);
    }

    [HttpPost("{appId}/{aadhaarNumber}/lock")]
    public async Task<IActionResult> LockDevice(string appId, string aadhaarNumber)
    {
        var filter = Builders<Device>.Filter.Where(d => d.AppId == appId && d.AadhaarNumber == aadhaarNumber);
        var update = Builders<Device>.Update.Set(d => d.LockStatus, "Locked");
        var options = new FindOneAndUpdateOptions<Device> { ReturnDocument = ReturnDocument.After };
        
        var device = await _db.Devices.FindOneAndUpdateAsync(filter, update, options);
        if (device == null) return NotFound("Device not found.");

        Console.WriteLine($"Triggering FCM Push: LOCK {aadhaarNumber} on App {appId}");
        return Ok(device);
    }

    [HttpPost("{appId}/{aadhaarNumber}/unlock")]
    public async Task<IActionResult> UnlockDevice(string appId, string aadhaarNumber)
    {
        var filter = Builders<Device>.Filter.Where(d => d.AppId == appId && d.AadhaarNumber == aadhaarNumber);
        var update = Builders<Device>.Update.Set(d => d.LockStatus, "Unlocked");
        var options = new FindOneAndUpdateOptions<Device> { ReturnDocument = ReturnDocument.After };

        var device = await _db.Devices.FindOneAndUpdateAsync(filter, update, options);
        if (device == null) return NotFound("Device not found.");

        Console.WriteLine($"Triggering FCM Push: UNLOCK {aadhaarNumber} on App {appId}");
        return Ok(device);
    }
}
