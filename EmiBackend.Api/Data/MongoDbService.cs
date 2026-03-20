using EmiBackend.Api.Models;
using Microsoft.Extensions.Configuration;
using MongoDB.Driver;

namespace EmiBackend.Api.Data
{
    public class MongoDbService
    {
        private readonly IMongoDatabase _database;

        public MongoDbService(IConfiguration config)
        {
            var connectionString = config.GetConnectionString("MongoDb") ?? "mongodb://localhost:27017/EmiLockerDB";
            var mongoUrl = MongoUrl.Create(connectionString);
            var mongoClient = new MongoClient(mongoUrl);
            _database = mongoClient.GetDatabase(mongoUrl.DatabaseName ?? "EmiLockerDB");
        }

        public IMongoCollection<AdminUser> AdminUsers => _database.GetCollection<AdminUser>("AdminUsers");
        public IMongoCollection<Device> Devices => _database.GetCollection<Device>("Devices");
        public IMongoCollection<OtpState> OtpStates => _database.GetCollection<OtpState>("OtpStates");
    }
}
