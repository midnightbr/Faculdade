using GarantirExistenciaBd.Database;

namespace GarantirExistenciaBd
{
    public class Execucao
    {
        public static void Main()
        {
            Console.WriteLine("Verificando existencia do banco de dados... Aguarde!");
            GarantindoExistenciaBDCaninos garantirBd = new GarantindoExistenciaBDCaninos();
            garantirBd.GarantindoExistencia();
            Console.WriteLine("Garantia efetuada com sucesso...");
            Console.ReadLine();
        }
    }
}