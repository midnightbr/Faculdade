using Caninos.Models.Interfaces;

namespace Caninos.Models
{
    public class Common
    {
        private readonly IBreedService _breedService;
        private readonly IDogService _dogService;

        public Breed Breed { get; set; }
        public Dog Dog { get; set; }

        public List<Breed> ListBreeds { get; set; } = new List<Breed>();

        public List<Dog> ListDogs { get; set; } = new List<Dog>();

        public void List()
        {
            ListBreeds = _breedService.GetAll();
            ListDogs = _dogService.GetAll();
        }

    }
}
