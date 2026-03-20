using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using EmiBackend.Api.Data;
using EmiBackend.Api.Models;
using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;
using Microsoft.IdentityModel.Tokens;

namespace EmiBackend.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private readonly MongoDbService _db;
    private readonly IConfiguration _config;

    public AuthController(MongoDbService db, IConfiguration config)
    {
        _db = db;
        _config = config;
    }

    public record LoginRequest(string Username, string Password);
    public record OtpVerifyRequest(string Username, string OtpCode);

    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginRequest req)
    {
        var user = await _db.AdminUsers.Find(u => u.Username == req.Username).FirstOrDefaultAsync();
        if (user == null || !BCrypt.Net.BCrypt.Verify(req.Password, user.PasswordHash))
        {
            return Unauthorized(new { message = "Invalid credentials." });
        }

        var otp = new Random().Next(100000, 999999).ToString();
        Console.WriteLine($"\n==========================================");
        Console.WriteLine($"MOCK SMS: Your EMI Locker OTP is {otp}");
        Console.WriteLine($"==========================================\n");

        await _db.OtpStates.InsertOneAsync(new OtpState
        {
            Username = user.Username,
            Code = otp,
            ExpiresAt = DateTime.UtcNow.AddMinutes(5),
            IsUsed = false
        });

        return Ok(new { message = "OTP sent successfully.", requiresOtp = true, mockOtp = otp });
    }

    [HttpPost("verify-otp")]
    public async Task<IActionResult> VerifyOtp([FromBody] OtpVerifyRequest req)
    {
        var state = await _db.OtpStates
            .Find(o => o.Username == req.Username && !o.IsUsed && o.ExpiresAt > DateTime.UtcNow)
            .SortByDescending(o => o.Id)
            .FirstOrDefaultAsync();

        if (state == null || state.Code != req.OtpCode)
        {
            return BadRequest(new { message = "Invalid or expired OTP." });
        }

        var filter = Builders<OtpState>.Filter.Eq(o => o.Id, state.Id);
        var update = Builders<OtpState>.Update.Set(o => o.IsUsed, true);
        await _db.OtpStates.UpdateOneAsync(filter, update);

        var jwtKey = _config["Jwt:Key"] ?? "super_secret_key_for_emi_locker_app_12345!";
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey));
        var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);
        
        var claims = new[] { new Claim(ClaimTypes.Name, req.Username) };
        
        var token = new JwtSecurityToken(
            claims: claims,
            expires: DateTime.UtcNow.AddHours(2),
            signingCredentials: creds
        );

        return Ok(new { token = new JwtSecurityTokenHandler().WriteToken(token) });
    }
}
