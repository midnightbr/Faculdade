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
    public class BreedsController : Controller
    {
        private readonly DataContext _context;

        public BreedsController(DataContext context)
        {
            _context = context;
        }

        // GET: Breeds
        public async Task<IActionResult> Index()
        {
              return View(await _context.Breeds.ToListAsync());
        }

        // GET: Breeds/Details/5
        public async Task<IActionResult> Details(int? id)
        {
            if (id == null || _context.Breeds == null)
            {
                return NotFound();
            }

            var breed = await _context.Breeds
                .FirstOrDefaultAsync(m => m.Id == id);
            if (breed == null)
            {
                return NotFound();
            }

            return View(breed);
        }

        // GET: Breeds/Create
        public IActionResult Create()
        {
            return View();
        }

        // POST: Breeds/Create
        // To protect from overposting attacks, enable the specific properties you want to bind to.
        // For more details, see http://go.microsoft.com/fwlink/?LinkId=317598.
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Create([Bind("Id,Name")] Breed breed)
        {
            if (ModelState.IsValid)
            {
                _context.Add(breed);
                await _context.SaveChangesAsync();
                return RedirectToRoute(new { controller = "Home", action = "Index" });
            }
            return View(breed);
        }

        // GET: Breeds/Edit/5
        public async Task<IActionResult> Edit(int? id)
        {
            if (id == null || _context.Breeds == null)
            {
                return NotFound();
            }

            var breed = await _context.Breeds.FindAsync(id);
            if (breed == null)
            {
                return NotFound();
            }
            return View(breed);
        }

        // POST: Breeds/Edit/5
        // To protect from overposting attacks, enable the specific properties you want to bind to.
        // For more details, see http://go.microsoft.com/fwlink/?LinkId=317598.
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Edit(int id, [Bind("Id,Name")] Breed breed)
        {
            if (id != breed.Id)
            {
                return NotFound();
            }

            if (ModelState.IsValid)
            {
                try
                {
                    _context.Update(breed);
                    await _context.SaveChangesAsync();
                }
                catch (DbUpdateConcurrencyException)
                {
                    if (!BreedExists(breed.Id))
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
            return View(breed);
        }

        // GET: Breeds/Delete/5
        public async Task<IActionResult> Delete(int? id)
        {
            if (id == null || _context.Breeds == null)
            {
                return NotFound();
            }

            var breed = await _context.Breeds
                .FirstOrDefaultAsync(m => m.Id == id);
            if (breed == null)
            {
                return NotFound();
            }

            return View(breed);
        }

        // POST: Breeds/Delete/5
        [HttpPost, ActionName("Delete")]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteConfirmed(int id)
        {
            if (_context.Breeds == null)
            {
                return Problem("Entity set 'DataContext.Breeds'  is null.");
            }
            var breed = await _context.Breeds.FindAsync(id);
            if (breed != null)
            {
                _context.Breeds.Remove(breed);
            }
            
            await _context.SaveChangesAsync();
            return RedirectToAction(nameof(Index));
        }

        private bool BreedExists(int id)
        {
          return _context.Breeds.Any(e => e.Id == id);
        }
    }
}
