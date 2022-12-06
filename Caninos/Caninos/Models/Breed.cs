using System.ComponentModel;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Caninos.Models
{
    public class Breed
    {
        [Key]
        public int Id { get; set; }

        [Required]
        public string Name { get; set; }

        public Breed() { }

        public Breed(int id, string name)
        {
            Id = id;
            Name = name;
        }
    }
}