using Caninos.DataBase;
using Caninos.Models.Interfaces;
using Microsoft.EntityFrameworkCore;

namespace Caninos.Models.Services
{
    public class DogService : IDogService
    {
        private readonly DataContext _context;

        public DogService(DataContext context)
        {
            _context = context;
        }

        public List<Dog> GetAll()
        {
            return _context.Dogs.ToList();
        }

        public Dog Insert(Dog dog)
        {
            _context.Dogs.Add(dog);
            _context.SaveChanges();
            return dog;
        }
    }
}
