/**
 * 
 */
/**
 * @author Marcos Henrique
 *
 */
package Funcionario.DAO;


import DatabaseConnection.Conexao;
import Funcionario.List.Funcionario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
//import java.sql.Statement;
//import java.util.logging.Level;
//import java.util.logging.Logger;

public class FuncionarioDAO {
	
	public void inserirDados(Funcionario funcionario) throws SQLException, ClassNotFoundException {
		// Conectando ao BD
		Connection bd = Conexao.conector();
		// Apontamento SQL pr�-compilado
		PreparedStatement prepStmt = null;
		try {
			prepStmt = bd.prepareStatement("INSERT INTO funcionarios VALUES (DEFAULT, ?, ?, ?)");
			prepStmt.setString(1, funcionario.getNome());
			prepStmt.setInt(2, funcionario.getSalario());
			prepStmt.setString(3, funcionario.getCargo());
			prepStmt.executeUpdate();
                        JOptionPane.showMessageDialog(null, "Pessoa adicionada com Sucesso!");
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (prepStmt != null)
				try {
					prepStmt.close();
				} catch (SQLException ignore) {
					
				}
		}
	}
	
	public void deletarDados (Funcionario f) throws SQLException, ClassNotFoundException {
		// Conectando ao BD
		Connection bd = Conexao.conector();
		
		PreparedStatement stmt = null;
		try {
			stmt = bd.prepareStatement("DELETE FROM funcionarios WHERE id = ?");
			
			stmt.setInt(1, f.getId());
			stmt.executeUpdate();
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			
			if (bd != null)
				try {
					bd.close();
				} catch (SQLException ignore) {
					
				}
		}
	}
	
	public void atualizarDadoNome(Funcionario f) throws SQLException, ClassNotFoundException {
		Connection bd = Conexao.conector();
		PreparedStatement stmt = null;
		
		try {
			stmt = bd.prepareStatement("UPDATE funcionarios SET nome = ? WHERE id = ?");
			stmt.setString(1, f.getNome());
			stmt.setInt(2, f.getId());
			stmt.executeUpdate();
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			
			if (bd != null)
				try {
					bd.close();
				} catch (SQLException ignore) {
					
				}
		}
	}
	
	public void atualizarDadoSalario(Funcionario f) throws SQLException, ClassNotFoundException {
		Connection bd = Conexao.conector();
		PreparedStatement stmt = null;
		
		try {
			stmt = bd.prepareStatement("UPDATE funcionarios SET salario = ? WHERE id = ?");
			stmt.setInt(1, f.getSalario());
			stmt.setInt(2, f.getId());
			stmt.executeUpdate();
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			
			if (bd != null)
				try {
					bd.close();
				} catch (SQLException ignore) {
					
				}
		}
	}
	
	public void atualizarDadoCargo(Funcionario f) throws SQLException, ClassNotFoundException {
		Connection bd = Conexao.conector();
		PreparedStatement stmt = null;
		
		try {
			stmt = bd.prepareStatement("UPDATE funcionarios SET cargo = ? WHERE id = ?");
			stmt.setString(1, f.getCargo());
			stmt.setInt(2, f.getId());
			stmt.executeUpdate();
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			
			if (bd != null)
				try {
					bd.close();
				} catch (SQLException ignore) {
					
				}
		}
	}
	
	public void atualizarDado(Funcionario f) throws SQLException, ClassNotFoundException {
		Connection bd = Conexao.conector();
		PreparedStatement stmt = null;
		
		try {
			stmt = bd.prepareStatement("UPDATE funcionarios SET nome = ?, salario = ?, cargo = ? WHERE id = ?");
			stmt.setString(1, f.getNome());
			stmt.setInt(2, f.getSalario());
			stmt.setString(3, f.getCargo());
			stmt.setInt(4, f.getId());
			stmt.executeUpdate();
                        JOptionPane.showMessageDialog(null, "Dados atualizados com Sucesso!");
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			
			if (bd != null)
				try {
					bd.close();
				} catch (SQLException ignore) {
					
				}
		}
	}
	
	public List<Funcionario> listarDados() throws SQLException, ClassNotFoundException {
		Connection bd = Conexao.conector();
		PreparedStatement stmt = null;
		ResultSet res = null;
		
		List<Funcionario> listaDeFuncionarios = new ArrayList<>();
		try {
			stmt = bd.prepareStatement("SELECT * FROM funcionarios");
			res = stmt.executeQuery();
			
			while (res.next()) {
				Funcionario funObj = new Funcionario();
				funObj.setId(res.getInt("id"));
				funObj.setNome(res.getString("nome"));
				funObj.setSalario(res.getInt("salario"));
				funObj.setCargo(res.getString("cargo"));
				listaDeFuncionarios.add(funObj);
			}
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (res != null)
				try {
					res.close();
				} catch (SQLException ignore) {
					
				}
			
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			
			if (bd != null) 
				try {
					bd.close();
				} catch (SQLException ignore) {
					
				}
		}
		
		return listaDeFuncionarios;
	}
        
        public List<Funcionario> pesquisaFun(String funcionario) throws SQLException, ClassNotFoundException {
		Connection bd = Conexao.conector();
                Funcionario f = new Funcionario();
		PreparedStatement stmt = null;
		ResultSet res = null;
                //String funcionario = Funcionario.getNome();
		
		List<Funcionario> dadosFuncionario = new ArrayList<>();
		try {
			stmt = bd.prepareStatement("SELECT * FROM funcionarios WHERE nome LIKE ?");
                        funcionario = "%" + funcionario + "%";
                        System.out.println(funcionario);
			stmt.setString(1, funcionario);
                        System.out.println(stmt);
                        res = stmt.executeQuery();
			
			while (res.next()) {
				Funcionario funObj = new Funcionario();
				funObj.setId(res.getInt("id"));
				funObj.setNome(res.getString("nome"));
				funObj.setSalario(res.getInt("salario"));
				funObj.setCargo(res.getString("cargo"));
				dadosFuncionario.add(funObj);
			}
		} catch (SQLException ex) {
			System.out.println(ex);
		} finally {
			if (res != null)
				try {
					res.close();
				} catch (SQLException ignore) {
					
				}
			
			if (stmt != null)
				try {
					stmt.close();
				} catch (SQLException ignore) {
					
				}
			
			if (bd != null) 
				try {
					bd.close();
				} catch (SQLException ignore) {
					
				}
		}
		
		return dadosFuncionario;
	}
//        public Funcionario pesquisarFuncionario(Funcionario pesqFun) throws SQLException, ClassNotFoundException {
////            Connection bd = Conexao.conector();
////            PreparedStatement stmt = null;
////            //Statement stm = null;
////            ResultSet res = null;
////            Funcionario funObj = new Funcionario();
////            
////            try {
////                
////                stmt = bd.prepareStatement("SELECT * FROM funcionarios WHERE nome like'%"+funObj.getPesqFun()+"%'");
////                res = stmt.executeQuery();
////                res.first();
////                while (res.next()) {
////                    //Funcionario fun = new Funcionario();
////                    funObj.setId(res.getInt("id"));
////                    funObj.setNome(res.getString("nome"));
////                    funObj.setSalario(res.getInt("salario"));
////                    funObj.setCargo(res.getString("cargo"));
////                    pesqFun = funObj;
////                }
////                 
////            } catch (SQLException ex) {
////                JOptionPane.showMessageDialog(null, "Erro ao tentar conexão com o BD.\n" + ex);
////            } finally {
////                if (res != null)
////                    try {
////                        res.close();
////                        
////                    } catch (SQLException ignore) {
////                        
////                    }
////                
////		if (stmt != null)
////                    try {
////			stmt.close();
////                    } catch (SQLException ignore) {
////					
////                    }
////			
////		if (bd != null)
////                    try {
////                        bd.close();
////                    } catch (SQLException ignore) {
////					
////                    }
////		}
////             
////            return pesqFun;
//
//           
//        }
        
}

