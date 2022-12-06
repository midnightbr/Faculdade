using Caninos.DataBase;
using Caninos.Models.Interfaces;
using Caninos.Models.Services;
using GarantirExistenciaDoBancoDeDados;
using Microsoft.EntityFrameworkCore;

namespace Caninos
{
    public class Program
    {
        public static void Main(string[] args)
        {
            BDPostgreSql postgreSql = new BDPostgreSql();
            postgreSql.Execucao("midnight", "beta2209", "192.168.1.100", "caninos");

            var builder = WebApplication.CreateBuilder(args);

            // Add services to the container.
            builder.Services.AddControllersWithViews();

            builder.Services.AddEntityFrameworkNpgsql().AddDbContext<DataContext>(options => options.UseNpgsql("Host=192.168.1.100;Port=5432;Pooling=true;Database=caninos;User Id=midnight;Password=beta2209;"));
            builder.Services.AddScoped<IDogService, DogService>();
            builder.Services.AddScoped<IBreedService, BreedService>();

            var app = builder.Build();

            // Configure the HTTP request pipeline.
            if (!app.Environment.IsDevelopment())
            {
                app.UseExceptionHandler("/Home/Error");
                // The default HSTS value is 30 days. You may want to change this for production scenarios, see https://aka.ms/aspnetcore-hsts.
                app.UseHsts();
            }

            app.UseHttpsRedirection();
            app.UseStaticFiles();

            app.UseRouting();

            app.UseAuthorization();

            app.MapControllerRoute(
                name: "default",
                pattern: "{controller=Home}/{action=Index}/{id?}");

            app.Run();
        }
    }
}