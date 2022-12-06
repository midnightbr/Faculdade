using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Caninos.Models
{
    public class Dog
    {
        [Key]
        [Required]
        public int Id { get; set; }

        [Required]
        public string Name { get; set; }

        [Required]
        public string Sexo { get; set; }

        public Breed Breed { get; set; }

        [Required]
        public int BreedId { get; set; }

        public Dog() { }

        public Dog(int id, string name, string sexo, Breed breed)
        {
            Id = id;
            Name = name;
            Sexo = sexo;
            Breed = breed;
        }
    }
}
