using System;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace EmiBackend.Api.Models
{
    public class OtpState
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string? Id { get; set; }

        public string Username { get; set; } = string.Empty;
        public string Code { get; set; } = string.Empty;
        public DateTime ExpiresAt { get; set; }
        public bool IsUsed { get; set; }
    }
}
