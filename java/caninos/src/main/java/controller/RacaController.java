package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import com.google.gson.Gson;
import dao.RacaJPADao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Raca;

/**
 * Servlet implementation class RacaController
 */
public class RacaController extends jakarta.servlet.http.HttpServlet {
	private static final long serialVersionUID = 1L;
	private Gson gson = new Gson();
	private RacaJPADao racaJPADao = new RacaJPADao();
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		
		List<Raca> racas = racaJPADao.getAll();
		String jsonCaes = gson.toJson(racas);
		out.println(jsonCaes);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		
		StringBuilder json = new StringBuilder();
		BufferedReader reader = request.getReader();
		String linha;
		while((linha = reader.readLine()) != null) {
			json.append(linha);
		}
		
		String sJson = json.toString();
		Raca raca = gson.fromJson(sJson, Raca.class);
		
		racaJPADao.add(raca);
		
		String mensagem = "Raça inserida com sucesso!";
		String jsonMensagem = "{" + "\"mensagem\":\"" + mensagem + "\"" + "}";
		
		out.println("[" + sJson + ", " + jsonMensagem + "]");
	}
	
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		
		StringBuilder json = new StringBuilder();
		BufferedReader reader = request.getReader();
		String linha;
		while((linha = reader.readLine()) != null) {
			json.append(linha);
		}
		
		String sJson = json.toString();
		Raca raca = gson.fromJson(sJson, Raca.class);
		
		racaJPADao.delete(raca);
		
		String mensagem = "Raça excluida com sucesso!";
		String jsonMensagem = "{" + "\"mensagem\":\"" + mensagem + "\"" + "}";
		
		out.println("[" + sJson + "," + jsonMensagem + "]0");
	}
	
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		
		StringBuilder json = new StringBuilder();
		BufferedReader reader = request.getReader();
		String linha;
		while((linha = reader.readLine()) != null) {
			json.append(linha);
		}
		
		String sJson = json.toString();
		Raca raca = gson.fromJson(sJson, Raca.class);
		
		racaJPADao.update(raca);
		
		String mensagem = "Raça atualizada com sucesso!";
		String jsonMensagem = "{" + "\"mensagem\":\"" + mensagem + "\"" + "}";
		
		out.println("[" + sJson + ", " + jsonMensagem + "]");
	}
}
