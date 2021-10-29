package controller;

import model.Cao;
import model.Raca;
import dao.CaoDAO;
import dao.RacaDAO;
import java.util.List;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class CaninoController
 */
public class CaninoController extends HttpServlet {
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		
		Gson gson = new Gson();
		CaoDAO caoDao = new CaoDAO();
		RacaDAO racaDao = new RacaDAO();
		
		// Recebendo parametros para saber o que fazer neste controller
		String acao = request.getParameter("acao");
		PrintWriter out = response.getWriter();
		
		switch(acao) {
		case "GET_CAES":
			List<Cao> caes = caoDao.getAll();
			String jsonCaes = gson.toJson(caes);
			out.println(jsonCaes);
			break;
			
		case "GET_RACAS":
			List<Raca> racas = racaDao.getAll();
			String jsonRacas = gson.toJson(racas);
			out.println(jsonRacas);
			break;
			
		default:
			out.println("{" + "\"acao\":\"" + acao + "\"" + "}");
			break;
		}
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		
		Gson gson = new Gson();
		CaoDAO caoDao = new CaoDAO();
		RacaDAO racaDao = new RacaDAO();
		
		// Recebendo o parametro de ação para saber o que realizar
		String acao = request.getParameter("acao");
		
		PrintWriter out = response.getWriter();
		
		switch(acao) {
		
		case "INSERT_CAO":
			// Pegando os parametros do formulario
			int id_cao = Integer.parseInt(request.getParameter("id"));
			String nome_cao = request.getParameter("nome");
			String sexo_cao = request.getParameter("sexo");
			int id_raca_cao = Integer.parseInt(request.getParameter("raca"));
			
			// Gerando o objeto raça
			Raca raca = racaDao.getById(id_raca_cao);
			
			// Criando o objeto cao
			Cao cao = new Cao();
			cao.setId(id_cao);
			cao.setNome(nome_cao);
			cao.setSexo(sexo_cao);
			cao.setRaca(raca); // setando  o objeto raça no objeto cao
			
			caoDao.add(cao);
			
			String jsonCao = gson.toJson(cao);
			String mensagem = "Canino inserido com sucesso!";
			String jsonMensagem = "{" + "\"mesagem\":\"" + mensagem + "\"" + "}";
			
			out.println("[" + jsonCao + ", " + jsonMensagem + "]");
			
			break;
			
		case "DELETE_CAO":
			// Pegando a ID do dog para excluir
			int id = Integer.parseInt(request.getParameter("id"));
			
			// Excluindo o dog
			int qtdExcluidos = caoDao.deleteById(id);
			
			out.println("{" + "\"mensagem\":\"" + qtdExcluidos + " registro(s) excluídos(s)" + "\"" + "}");
			
			break;
			
		case "INSERT_RACA":
			int id_raca = Integer.parseInt(request.getParameter("id_raca"));
			String nome_raca = request.getParameter("nome_raca");
			
			Raca insertRaca = new Raca();
			
			insertRaca.setId(id_raca);
			insertRaca.setNome(nome_raca);
			
			racaDao.inserirRaca(insertRaca);
		
			String jsonRaca = gson.toJson(insertRaca);
			mensagem = "Raça inserida com sucesso!";
			jsonMensagem = "{" + "\"mesagem\":\"" + mensagem + "\"" + "}";
			
			out.println("[" + jsonRaca + ", " + jsonMensagem + "]");
			
			break;
			
		case "DELETE_RACA":
			id_raca = Integer.parseInt(request.getParameter("id_raca"));
			int racaExcluida = racaDao.deleteById(id_raca);
			
			jsonRaca = gson.toJson(racaExcluida);
			mensagem = "Raça excluida com sucesso!";
			jsonMensagem = "{" + "\"mesagem\":\"" + mensagem + "\"" + "}"; 
			
			out.println("{" + "\"mensagem\":\"" + racaExcluida + " registro(s) excluídos(s)" + "\"" + "}");
			
			break;
			
		case "ALTER_RACA":
			id_raca = Integer.parseInt(request.getParameter("id_raca"));
//			String raca_id = request.getParameter("id_raca");
			String rac_nome = request.getParameter("new_nome_raca");
			
			Raca alterarRaca = new Raca();
			
			alterarRaca.setId(id_raca);
			alterarRaca.setNome(rac_nome);
			
			racaDao.alterarById(alterarRaca);
			
			jsonRaca = gson.toJson(alterarRaca);
			mensagem = "Raça alterado com sucesso!";
			jsonMensagem = "{" + "\"mesagem\":\"" + mensagem + "\"" + "}";
			
			out.println("[" + jsonRaca + ", " + jsonMensagem + "]");
			
			break;
			
		default:
			out.println("{" + "\"acao\":\"" + acao + "\"" + "}");
			
		}
		
		
	}
	
}
