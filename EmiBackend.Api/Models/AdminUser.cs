using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace EmiBackend.Api.Models
{
    public class AdminUser
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string? Id { get; set; }

        public string AppId { get; set; } = "DEFAULT_APP"; // Multiple Apps support
        public string Username { get; set; } = string.Empty;
        public string PasswordHash { get; set; } = string.Empty;
        public string MobileNumber { get; set; } = string.Empty;
    }
}
