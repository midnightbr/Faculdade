using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Rendering;
using Microsoft.EntityFrameworkCore;
using Caninos.DataBase;
using Caninos.Models;

namespace Caninos.Controllers
{
    public class DogsController : Controller
    {
        private readonly DataContext _context;

        public DogsController(DataContext context)
        {
            _context = context;
        }

        // GET: Dogs
        public async Task<IActionResult> Index()
        {
            var dataContext = _context.Dogs.Include(d => d.Breed);
            return View(await dataContext.ToListAsync());
        }

        // GET: Dogs/Details/5
        public async Task<IActionResult> Details(int? id)
        {
            if (id == null || _context.Dogs == null)
            {
                return NotFound();
            }

            var dog = await _context.Dogs
                .Include(d => d.Breed)
                .FirstOrDefaultAsync(m => m.Id == id);
            if (dog == null)
            {
                return NotFound();
            }

            return View(dog);
        }

        // GET: Dogs/Create
        public IActionResult Create()
        {
            ViewData["BreedId"] = new SelectList(_context.Breeds, "Id", "Name");
            return View();
        }

        // POST: Dogs/Create
        // To protect from overposting attacks, enable the specific properties you want to bind to.
        // For more details, see http://go.microsoft.com/fwlink/?LinkId=317598.
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Create([Bind("Id,Name,Sexo,BreedId")] Dog dog)
        {
            if (ModelState.IsValid)
            {
                _context.Add(dog);
                await _context.SaveChangesAsync();
                return RedirectToRoute(new { controller = "Home", action = "Index"});
            }
            ViewData["BreedId"] = new SelectList(_context.Breeds, "Id", "Name", dog.BreedId);
            return View(dog);
        }

        // GET: Dogs/Edit/5
        public async Task<IActionResult> Edit(int? id)
        {
            if (id == null || _context.Dogs == null)
            {
                return NotFound();
            }

            var dog = await _context.Dogs.FindAsync(id);
            if (dog == null)
            {
                return NotFound();
            }
            ViewData["BreedId"] = new SelectList(_context.Breeds, "Id", "Name", dog.BreedId);
            return View(dog);
        }

        // POST: Dogs/Edit/5
        // To protect from overposting attacks, enable the specific properties you want to bind to.
        // For more details, see http://go.microsoft.com/fwlink/?LinkId=317598.
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Edit(int id, [Bind("Id,Name,Sexo,BreedId")] Dog dog)
        {
            if (id != dog.Id)
            {
                return NotFound();
            }

            if (ModelState.IsValid)
            {
                try
                {
                    _context.Update(dog);
                    await _context.SaveChangesAsync();
                }
                catch (DbUpdateConcurrencyException)
                {
                    if (!DogExists(dog.Id))
                    {
                        return NotFound();
                    }
                    else
                    {
                        throw;
                    }
                }
                return RedirectToAction(nameof(Index));
            }
            ViewData["BreedId"] = new SelectList(_context.Breeds, "Id", "Name", dog.BreedId);
            return View(dog);
        }

        // GET: Dogs/Delete/5
        public async Task<IActionResult> Delete(int? id)
        {
            if (id == null || _context.Dogs == null)
            {
                return NotFound();
            }

            var dog = await _context.Dogs
                .Include(d => d.Breed)
                .FirstOrDefaultAsync(m => m.Id == id);
            if (dog == null)
            {
                return NotFound();
            }

            return View(dog);
        }

        // POST: Dogs/Delete/5
        [HttpPost, ActionName("Delete")]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteConfirmed(int id)
        {
            if (_context.Dogs == null)
            {
                return Problem("Entity set 'DataContext.Dogs'  is null.");
            }
            var dog = await _context.Dogs.FindAsync(id);
            if (dog != null)
            {
                _context.Dogs.Remove(dog);
            }
            
            await _context.SaveChangesAsync();
            return RedirectToAction(nameof(Index));
        }

        private bool DogExists(int id)
        {
          return _context.Dogs.Any(e => e.Id == id);
        }
    }
}
