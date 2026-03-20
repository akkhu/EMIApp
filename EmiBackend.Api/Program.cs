using System.Text;
using EmiBackend.Api.Data;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using MongoDB.Driver;
using Microsoft.IdentityModel.Tokens;
using EmiBackend.Api.Models;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Configure MongoDB Service
builder.Services.AddSingleton<MongoDbService>();

// Configure JWT Authentication
var jwtKey = builder.Configuration["Jwt:Key"] ?? "super_secret_key_for_emi_locker_app_12345!";
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = false,
            ValidateAudience = false,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey))
        };
    });

builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowReactApp", policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

var app = builder.Build();

// Seed dummy admin user and devices in MongoDB
using (var scope = app.Services.CreateScope())
{
    var mongoDb = scope.ServiceProvider.GetRequiredService<MongoDbService>();
    
    // Check if empty
    var adminCount = mongoDb.AdminUsers.CountDocuments(FilterDefinition<AdminUser>.Empty);
    if (adminCount == 0)
    {
        mongoDb.AdminUsers.InsertOne(new AdminUser {
            AppId = "EMI_PRO",
            Username = "admin",
            PasswordHash = BCrypt.Net.BCrypt.HashPassword("password123"),
            MobileNumber = "1234567890"
        });
    }

    var devicesCount = mongoDb.Devices.CountDocuments(FilterDefinition<Device>.Empty);
    if (devicesCount == 0)
    {
        mongoDb.Devices.InsertOne(new Device {
            AppId = "EMI_PRO",
            Imei = "123456789012345",
            CustomerName = "John Doe",
            LockStatus = "Unlocked",
            LastSyncTime = DateTime.UtcNow
        });
        mongoDb.Devices.InsertOne(new Device {
            AppId = "EMI_LITE",
            Imei = "987654321098765",
            CustomerName = "Jane Smith",
            LockStatus = "Locked",
            LastSyncTime = DateTime.UtcNow
        });
    }
}

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseCors("AllowReactApp");
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

app.Run();
