/**
 * 
 */
/**
 * @author Marcos Henrique
 *
 */
package Funcionario.List;

public class Funcionario {
	private Integer id;
	private String nome;
	private Integer salario;
	private String cargo;
//        private String pesqFun;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getNome() {
		return nome;
	}
	
	public void setNome(String nome) {
		this.nome = nome;
	}
	
	public Integer getSalario() {
		return salario;
	}
	
	public void setSalario(Integer salario) {
		this.salario = salario;
	}
	
	public String getCargo() {
		return cargo;
	}
	
	public void setCargo(String cargo) {
		this.cargo = cargo;
	}
        
//        public String getPesqFun() {
//            return pesqFun;
//        }
//        
//        public void setPesqFun(String pesqFun) {
//            this.pesqFun = pesqFun;
//        }
}