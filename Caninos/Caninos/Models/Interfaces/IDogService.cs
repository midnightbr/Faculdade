namespace Caninos.Models.Interfaces;

public interface IDogService
{
    List<Dog> GetAll();
    Dog Insert(Dog dog);
}