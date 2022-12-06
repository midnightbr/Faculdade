using Caninos.Models;
using Microsoft.AspNetCore.Mvc;
using System.Diagnostics;
using Caninos.DataBase;
using Caninos.Models.Interfaces;
using Microsoft.EntityFrameworkCore;

namespace Caninos.Controllers
{
    public class HomeController : Controller
    {
        private readonly ILogger<HomeController> _logger;
        private readonly DataContext _context;
        private readonly IDogService _dogService;
        private readonly IBreedService _breedService;

        public HomeController(DataContext context, IDogService dogService, IBreedService breedService, ILogger<HomeController> logger)
        {
            _logger = logger;
            _dogService = dogService;
            _breedService = breedService;
        }

        public async Task<IActionResult> Index(string search, string column)
        {
            Common common = new Common();
            common.ListDogs = _dogService.GetAll();

            if (column == "Dog")
            {
                var dogs = from dog in _context.Dogs select dog;
                if (!string.IsNullOrEmpty(search))
                {
                    dogs = dogs.Where(x => x.Name.Contains(search));
                    common.ListDogs = await dogs.ToListAsync();
                }
            }
            if (column == "Breed")
            {
                var dogs = from dog in _context.Dogs select dog;
                if (!string.IsNullOrEmpty(search))
                {
                    dogs = dogs.Where(x => x.Breed.Name.Contains(search));
                    common.ListDogs = await dogs.ToListAsync();
                }
            }

            common.ListBreeds = _breedService.GetAll();
            return View(common);
        }

        public IActionResult Privacy()
        {
            return View();
        }

        [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
        public IActionResult Error()
        {
            return View(new ErrorViewModel { RequestId = Activity.Current?.Id ?? HttpContext.TraceIdentifier });
        }
    }
}