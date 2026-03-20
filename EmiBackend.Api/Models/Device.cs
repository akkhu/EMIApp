using System;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace EmiBackend.Api.Models
{
    public class Device
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string? Id { get; set; }

        public string AppId { get; set; } = "DEFAULT_APP";
        public string Imei { get; set; } = string.Empty;
        public string CustomerName { get; set; } = string.Empty;
        public string LockStatus { get; set; } = "Unlocked";
        public DateTime LastSyncTime { get; set; }
    }
}
