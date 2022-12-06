using Caninos.Models;
using Microsoft.EntityFrameworkCore;

namespace Caninos.DataBase
{
    public class DataContext : DbContext
    {
        public DataContext(DbContextOptions<DataContext> options) : base(options) { }

        public DbSet<Dog> Dogs { get; set; }
        public DbSet<Breed> Breeds { get; set; }

    }
}
