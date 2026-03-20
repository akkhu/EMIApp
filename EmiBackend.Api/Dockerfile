FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS base
WORKDIR /app
EXPOSE 8080

FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src
COPY EmiBackend.Api.csproj .
RUN dotnet restore EmiBackend.Api.csproj
COPY . .
RUN dotnet build EmiBackend.Api.csproj -c Release -o /app/build

FROM build AS publish
RUN dotnet publish EmiBackend.Api.csproj -c Release -o /app/publish /p:UseAppHost=false

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .

# Render dynamically sets PORT env var. ASP.NET 8 defaults to 8080 which matches seamlessly.
ENV ASPNETCORE_URLS=http://+:8080

ENTRYPOINT ["dotnet", "EmiBackend.Api.dll"]
