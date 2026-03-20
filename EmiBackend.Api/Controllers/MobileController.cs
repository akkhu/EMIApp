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

    [HttpGet("{appId}/{imei}/status")]
    public async Task<IActionResult> GetStatus(string appId, string imei)
    {
        var device = await _db.Devices
            .Find(d => d.AppId == appId && d.Imei == imei)
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
}
