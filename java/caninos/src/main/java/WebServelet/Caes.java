package WebServelet;

import java.io.IOException;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.servlet.ServletException;

import dao.*;
import model.*;
import java.io.*;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class Caes
 */
@WebServlet(urlPatterns = "/caes")
public class Caes extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter writer = response.getWriter();
		CaoDAO caoDao = new CaoDAO();
		List<Cao> caes = caoDao.getAll();
		
		writer.println("<html>");
		writer.println("<body>");
		writer.println("<h1>Listagem dos Caes Cadastrados</h1>");
		writer.println("<table>");
		writer.println("<tr>");
		writer.println("	<th>Id</th>");
		writer.println("	<th>Nome</th>");
		writer.println("	<th>Raca</th>");
		writer.println("	<th>Sexo</th>");
		writer.println("</tr>");
		
		for(Cao cao : caes) {
			writer.println("<tr>");
			writer.println("	<td>" + cao.getId() + "</td>");
			writer.println("	<td>" + cao.getNome() + "</td>");
			writer.println("	<td>" + cao.getRaca().getNome() + "</td>");
			writer.println("	<td>" + cao.getSexo() + "</td>");
			writer.println("</tr>");
		}
		
		writer.println("</table>");
		writer.println("</body>");
	}
}
