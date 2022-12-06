using Caninos.DataBase;
using Caninos.Models.Interfaces;

namespace Caninos.Models.Services
{
    public class BreedService : IBreedService
    {
        private readonly DataContext _context;

        public BreedService(DataContext context)
        {
            _context = context;
        }

        public List<Breed> GetAll()
        {
            return _context.Breeds.OrderBy(x => x.Name).ToList();
        }

        public Breed Insert(Breed breed)
        {
            _context.Breeds.Add(breed);
            _context.SaveChanges();
            return breed;
        }
    }
}
