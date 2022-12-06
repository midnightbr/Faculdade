namespace Caninos.Models.Interfaces;

public interface IBreedService
{
    List<Breed> GetAll();
    Breed Insert(Breed breed);
}