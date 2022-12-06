using System;
using System.Collections.Generic;
using System.Data.Common;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Npgsql;

namespace GarantirExistenciaBd.Database
{
    internal class GarantindoExistenciaBDCaninos
    {
        public bool GarantindoExistencia()
        {
            try
            {
                NpgsqlConnection connection = new NpgsqlConnection(
                    "Host=192.168.1.100;Port=5432;Pooling=true;Database=caninos;User Id=midnight;Password=beta2209;");
                connection.Open();
                Console.WriteLine("Banco já existe...");
            }
            catch (NpgsqlException ex)
            {
                NpgsqlConnection connection = new NpgsqlConnection(
                    "Host=192.168.1.100;Port=5432;Pooling=true;Database=postgres;User Id=midnight;Password=beta2209;");
                connection.Open();
                string sql = "CREATE DATABASE Caninos";
                NpgsqlCommand cmd = new NpgsqlCommand(sql, connection);
                cmd.ExecuteNonQuery();
                cmd.Clone();
                Console.WriteLine("Banco criado com sucesso!");
            }
            return true;
        }
    }
}
