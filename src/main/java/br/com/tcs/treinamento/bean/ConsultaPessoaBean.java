package br.com.tcs.treinamento.bean;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

// Imports PrimeFaces
import org.primefaces.PrimeFaces;

// Imports específicos para PDF (iText)
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

// Imports específicos para Excel (Apache POI)
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import br.com.tcs.treinamento.entity.Pessoa;
import br.com.tcs.treinamento.service.PessoaService;
import br.com.tcs.treinamento.service.impl.PessoaServiceImpl;

@ManagedBean(name = "consultaPessoaBean")
@ViewScoped
public class ConsultaPessoaBean implements Serializable {

    private List<Pessoa> pessoas;
    private Pessoa pessoaSelecionada;
    private String errorMessage;
    private Long pessoaId;
    private Boolean tpManutencao;
    private String abaAtiva = "todas";

    private transient PessoaService pessoaService = new PessoaServiceImpl();

    @PostConstruct
    public void init() {
        // Recupera parâmetro "pessoaId" da URL
        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestParameterMap();
        String idParam = params.get("pessoaId");
        if (idParam != null && !idParam.trim().isEmpty()) {
            try {
                pessoaId = Long.valueOf(idParam);
                pessoaSelecionada = pessoaService.buscarPorId(pessoaId);
            } catch (NumberFormatException e) {
                errorMessage = "ID inválido da pessoa.";
            }
        }
        // Recupera o parâmetro tpManutencao; se não existir, assume um valor padrão (por exemplo, true para edição)
        String tpParam = params.get("tpManutencao");
        if (tpParam != null && !tpParam.trim().isEmpty()) {
            setTpManutencao(Boolean.valueOf(tpParam));
        } else {
            setTpManutencao(true);
        }
        pessoas = pessoaService.listar();
    }

    public String prepararEdicao(Pessoa pessoa) {
        this.pessoaSelecionada = pessoa;
        return "alterar?faces-redirect=true&pessoaId=" + pessoa.getId() + "&tpManutencao=true";
    }

    public String prepararExclusao(Pessoa pessoa) {
        this.pessoaSelecionada = pessoa;
        return "excluir?faces-redirect=true&pessoaId=" + pessoa.getId() + "&tpManutencao=false";
    }

    // Adicione estes métodos à sua classe ConsultaPessoaBean

    /**
     * Prepara a edição da renda mensal
     */
    public void prepararEdicaoRenda(Pessoa pessoa) {
        this.pessoaSelecionada = pessoa;
        // Cria uma cópia da pessoa para não modificar diretamente a original
        // até que a alteração seja confirmada
    }

    /**
     * Salva a nova renda mensal
     */
    public void salvarRendaMensal() {
        if (pessoaSelecionada != null) {
            try {
                Pessoa pessoaAtual = pessoaService.buscarPorId(pessoaSelecionada.getId());


                pessoaAtual.setRendaMensal(pessoaSelecionada.getRendaMensal());
                pessoaAtual.setDataManutencao(new Date());

                pessoaService.atualizar(pessoaAtual);

                pessoas = pessoaService.listar();

                pessoaSelecionada = null;

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Renda mensal atualizada com sucesso!"));

            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao atualizar renda mensal: " + e.getMessage()));
            }
        }
    }

    public String atualizarConsulta() {
        pessoaService.atualizar(pessoaSelecionada);
        pessoas = pessoaService.listar();
        return "consultaPessoas?faces-redirect=true";
    }

    public void limparAlteracoes() {
        if (pessoaSelecionada != null) {
            pessoaSelecionada = pessoaService.buscarPorId(pessoaSelecionada.getId());
        }
    }

    /**
     * Método que converte o VO para a entidade e chama o service para persistir.
     * Após persistir, exibe o popup de sucesso.
     */
    public void confirmar() {
        // Converte o VO para a entidade Pessoa
        Pessoa pessoa = mapPessoaEntity();
        // Chama o service para persistir a entidade
        try {
            pessoaService.atualizar(pessoa);
            // Exibe o popup de sucesso após a confirmação
            PrimeFaces.current().executeScript("PF('successDialog').show();");
        } catch (Exception e) {
            // Em caso de erro na persistência, exibe o diálogo de erro
            errorMessage = "Erro ao cadastrar pessoa: " + e.getMessage();
            PrimeFaces.current().executeScript("PF('errorDialog').show();");
            return;
        }
    }

    /**
     * mapPessoaEntity
     * Mapeamento da VO para Entity
     */
    private Pessoa mapPessoaEntity() {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(pessoaSelecionada.getId());
        pessoa.setNome(pessoaSelecionada.getNome());
        pessoa.setIdade(pessoaSelecionada.getIdade());
        pessoa.setEmail(pessoaSelecionada.getEmail());
        pessoa.setData(pessoaSelecionada.getData());
        pessoa.setPais(pessoaSelecionada.getPais());
        pessoa.setTipoDocumento(pessoaSelecionada.getTipoDocumento());
        pessoa.setRendaMensal(pessoaSelecionada.getRendaMensal());
        pessoa.setNumeroCPF(pessoaSelecionada.getNumeroCPF());
        pessoa.setNumeroCNPJ(pessoaSelecionada.getNumeroCNPJ());
        pessoa.setDataManutencao(new Date());
        pessoa.setAtivo(getTpManutencao());
        return pessoa;
    }

    public void confirmarExclusao(){
        Pessoa pessoa = mapPessoaEntity();
        try {
            pessoaService.atualizar(pessoa); //Exclusao logica
            //pessoaService.excluir(pessoa); // Exclusao fisica
            // Exibe o popup de sucesso após a confirmação
            PrimeFaces.current().executeScript("PF('successDialog').show();");
        } catch (Exception e) {
            // Em caso de erro na persistência, exibe o diálogo de erro
            errorMessage = "Erro ao cadastrar pessoa: " + e.getMessage();
            PrimeFaces.current().executeScript("PF('errorDialog').show();");
            return;
        }
    }

    public void limpar() {
        pessoaSelecionada.setNome(null);
        pessoaSelecionada.setIdade(null);
        pessoaSelecionada.setEmail(null);
        pessoaSelecionada.setData(null);
        pessoaSelecionada.setTipoDocumento(null);
        pessoaSelecionada.setNumeroCPF(null);
        pessoaSelecionada.setNumeroCNPJ(null);
        pessoaSelecionada.setRendaMensal(null);
        pessoaSelecionada.setPais(null);
        errorMessage = null;
    }

    public void validarCampos() {
        List<String> erros = new ArrayList<>();

        if (pessoaSelecionada.getNome() == null || pessoaSelecionada.getNome().trim().isEmpty()) {
            erros.add("Nome não informado.");
        }
        if (pessoaSelecionada.getIdade() == null) {
            erros.add("Idade não informada.");
        }
        if (pessoaSelecionada.getEmail() == null || pessoaSelecionada.getEmail().trim().isEmpty()) {
            erros.add("E-mail não informado.");
        }

        if (pessoaSelecionada.getPais() == null) {
            erros.add("País não informado.");
        }
        if (pessoaSelecionada.getData() == null) {
            erros.add("Data de nascimento não informada.");
        }
        if (pessoaSelecionada.getRendaMensal() == null || pessoaSelecionada.getRendaMensal() <= 0) {
            erros.add("Renda mensal não informada ou inválida.");
        }
        if (pessoaSelecionada.getTipoDocumento() == null || pessoaSelecionada.getTipoDocumento().trim().isEmpty()) {
            erros.add("Tipo de documento não informado.");
        } else {
            if ("CPF".equals(pessoaSelecionada.getTipoDocumento())) {
                if (pessoaSelecionada.getNumeroCPF() == null || pessoaSelecionada.getNumeroCPF().trim().isEmpty() ||
                        pessoaSelecionada.getNumeroCPF().trim().length() < 11) {
                    erros.add("CPF não informado ou incompleto (deve conter 11 dígitos).");
                }
            } else if ("CNPJ".equals(pessoaSelecionada.getTipoDocumento())) {
                if (pessoaSelecionada.getNumeroCNPJ() == null || pessoaSelecionada.getNumeroCNPJ().trim().isEmpty() ||
                        pessoaSelecionada.getNumeroCNPJ().trim().length() < 14) {
                    erros.add("CNPJ não informado ou incompleto (deve conter 14 dígitos).");
                }
            }
        }

        if (!erros.isEmpty()) {
            errorMessage = String.join("<br/>", erros);
            PrimeFaces.current().executeScript("PF('errorDialog').show();");
        } else {
            PrimeFaces.current().executeScript("PF('confirmDialog').show();");
        }
    }

    public void calcularIdade() {
        if (pessoaSelecionada != null && pessoaSelecionada.getData() != null) {
            LocalDate dataNascimento = pessoaSelecionada.getData().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            LocalDate dataAtual = LocalDate.now();

            int idade = Period.between(dataNascimento, dataAtual).getYears();

            pessoaSelecionada.setIdade(idade);
        } else if (pessoaSelecionada != null) {
            pessoaSelecionada.setIdade(null);
        }
    }

    public void exportarPdf() {
        OutputStream outputStream = null;
        try {
            System.out.println("Iniciando exportação PDF...");

            List<Pessoa> dadosParaExportar = filtrarPorAba();

            if (dadosParaExportar == null || dadosParaExportar.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", "Não há dados para exportar."));
                return;
            }

            String nomeArquivo = "pessoas_" + abaAtiva + "_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf";

            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

            // IMPORTANTE: Reset da resposta antes de configurar
            response.reset();
            response.setContentType("application/pdf");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + nomeArquivo + "\"");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            outputStream = response.getOutputStream();

            // Criar documento PDF
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            document.open();

            // Título
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Relatório de " + getTituloAba(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // Criar tabela
            PdfPTable table = new PdfPTable(10);
            table.setWidthPercentage(100);

            // Definir larguras das colunas
            float[] columnWidths = {15f, 8f, 20f, 12f, 15f, 15f, 12f, 12f, 8f, 8f};
            table.setWidths(columnWidths);

            // Cabeçalhos
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            addCellToTable(table, "Nome", headerFont);
            addCellToTable(table, "Idade", headerFont);
            addCellToTable(table, "Email", headerFont);
            addCellToTable(table, "Data Nasc.", headerFont);
            addCellToTable(table, "País", headerFont);
            addCellToTable(table, "CPF/CNPJ", headerFont);
            addCellToTable(table, "Renda Mensal", headerFont);
            addCellToTable(table, "Tipo Doc.", headerFont);
            addCellToTable(table, "Data Manut.", headerFont);
            addCellToTable(table, "Status", headerFont);

            // Adicionar dados
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 7);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

            for (Pessoa pessoa : dadosParaExportar) {
                addCellToTable(table, pessoa.getNome() != null ? pessoa.getNome() : "", dataFont);
                addCellToTable(table, pessoa.getIdade() != null ? pessoa.getIdade().toString() : "", dataFont);
                addCellToTable(table, pessoa.getEmail() != null ? pessoa.getEmail() : "", dataFont);
                addCellToTable(table, pessoa.getData() != null ? dateFormat.format(pessoa.getData()) : "", dataFont);
                addCellToTable(table, pessoa.getPais() != null ? pessoa.getPais() : "", dataFont);

                String documento = "";
                if (pessoa.getNumeroCPF() != null && !pessoa.getNumeroCPF().trim().isEmpty()) {
                    documento = pessoa.getNumeroCPF();
                } else if (pessoa.getNumeroCNPJ() != null && !pessoa.getNumeroCNPJ().trim().isEmpty()) {
                    documento = pessoa.getNumeroCNPJ();
                }
                addCellToTable(table, documento, dataFont);

                addCellToTable(table, pessoa.getRendaMensal() != null ?
                        String.format("R$ %.2f", pessoa.getRendaMensal()) : "R$ 0,00", dataFont);
                addCellToTable(table, pessoa.getTipoDocumento() != null ? pessoa.getTipoDocumento() : "", dataFont);
                addCellToTable(table, pessoa.getDataManutencao() != null ?
                        dateFormat.format(pessoa.getDataManutencao()) : "", dataFont);
                addCellToTable(table, pessoa.getAtivo() != null ?
                        (pessoa.getAtivo() ? "Ativo" : "Inativo") : "N/A", dataFont);
            }

            document.add(table);
            document.close();
            writer.close();

            outputStream.flush();
            outputStream.close();
            outputStream = null;

            System.out.println("PDF gerado com sucesso: " + nomeArquivo);
            facesContext.responseComplete();

        } catch (Exception e) {
            System.err.println("Erro na exportação PDF: " + e.getMessage());
            e.printStackTrace();

            // Limpar o output stream em caso de erro
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ioException) {
                    System.err.println("Erro ao fechar output stream: " + ioException.getMessage());
                }
            }

            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null && !context.getResponseComplete()) {
                context.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
                                "Erro ao exportar PDF: " + e.getMessage()));
            }
        }
    }

    /**
     * Exporta para Excel baseado na aba ativa
     */
    public void exportarExcel() {
        OutputStream outputStream = null;
        Workbook workbook = null;

        try {
            System.out.println("Iniciando exportação Excel...");

            List<Pessoa> dadosParaExportar = filtrarPorAba();

            if (dadosParaExportar == null || dadosParaExportar.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", "Não há dados para exportar."));
                return;
            }

            String nomeArquivo = "pessoas_" + abaAtiva + "_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".xlsx";

            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

            // IMPORTANTE: Reset da resposta antes de configurar
            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + nomeArquivo + "\"");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            outputStream = response.getOutputStream();

            // Criar workbook Excel
            workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet(getTituloAba());

            // Criar estilos
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(createHelper.createDataFormat().getFormat("R$ #,##0.00"));

            // Criar cabeçalhos
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Nome", "Idade", "Email", "Data Nascimento", "País",
                    "CPF/CNPJ", "Renda Mensal", "Tipo Documento", "Data Manutenção", "Status"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Adicionar dados
            int rowNum = 1;
            for (Pessoa pessoa : dadosParaExportar) {
                Row row = sheet.createRow(rowNum++);

                // Nome
                Cell nomeCell = row.createCell(0);
                nomeCell.setCellValue(pessoa.getNome() != null ? pessoa.getNome() : "");

                // Idade
                Cell idadeCell = row.createCell(1);
                if (pessoa.getIdade() != null) {
                    idadeCell.setCellValue(pessoa.getIdade());
                }

                // Email
                Cell emailCell = row.createCell(2);
                emailCell.setCellValue(pessoa.getEmail() != null ? pessoa.getEmail() : "");

                // Data de nascimento
                if (pessoa.getData() != null) {
                    Cell dateCell = row.createCell(3);
                    dateCell.setCellValue(pessoa.getData());
                    dateCell.setCellStyle(dateStyle);
                }

                // País
                Cell paisCell = row.createCell(4);
                paisCell.setCellValue(pessoa.getPais() != null ? pessoa.getPais() : "");

                // CPF/CNPJ
                Cell documentoCell = row.createCell(5);
                String documento = "";
                if (pessoa.getNumeroCPF() != null && !pessoa.getNumeroCPF().trim().isEmpty()) {
                    documento = pessoa.getNumeroCPF();
                } else if (pessoa.getNumeroCNPJ() != null && !pessoa.getNumeroCNPJ().trim().isEmpty()) {
                    documento = pessoa.getNumeroCNPJ();
                }
                documentoCell.setCellValue(documento);

                // Renda mensal
                Cell rendaCell = row.createCell(6);
                if (pessoa.getRendaMensal() != null) {
                    rendaCell.setCellValue(pessoa.getRendaMensal());
                    rendaCell.setCellStyle(currencyStyle);
                }

                // Tipo documento
                Cell tipoDocCell = row.createCell(7);
                tipoDocCell.setCellValue(pessoa.getTipoDocumento() != null ? pessoa.getTipoDocumento() : "");

                // Data manutenção
                if (pessoa.getDataManutencao() != null) {
                    Cell dataManutCell = row.createCell(8);
                    dataManutCell.setCellValue(pessoa.getDataManutencao());
                    dataManutCell.setCellStyle(dateStyle);
                }

                // Status
                Cell statusCell = row.createCell(9);
                statusCell.setCellValue(pessoa.getAtivo() != null ?
                        (pessoa.getAtivo() ? "Ativo" : "Inativo") : "N/A");
            }

            // Auto-ajustar largura das colunas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            workbook.close();
            workbook = null;

            outputStream.flush();
            outputStream.close();
            outputStream = null;

            System.out.println("Excel gerado com sucesso: " + nomeArquivo);
            facesContext.responseComplete();

        } catch (Exception e) {
            System.err.println("Erro na exportação Excel: " + e.getMessage());
            e.printStackTrace();

            // Limpar recursos em caso de erro
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException ioException) {
                    System.err.println("Erro ao fechar workbook: " + ioException.getMessage());
                }
            }

            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ioException) {
                    System.err.println("Erro ao fechar output stream: " + ioException.getMessage());
                }
            }

            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null && !context.getResponseComplete()) {
                context.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
                                "Erro ao exportar Excel: " + e.getMessage()));
            }
        }
    }

    /**
     * Métodos auxiliares
     */
    private void addCellToTable(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        cell.setBorder(PdfPCell.RECTANGLE);
        table.addCell(cell);
    }

    private List<Pessoa> filtrarPorAba() {
        if (pessoas == null || pessoas.isEmpty()) {
            System.out.println("Lista de pessoas está vazia ou nula");
            return new ArrayList<>();
        }

        List<Pessoa> resultado;

        switch (abaAtiva) {
            case "cpf":
                resultado = pessoas.stream()
                        .filter(p -> p.getNumeroCPF() != null && !p.getNumeroCPF().trim().isEmpty())
                        .collect(Collectors.toList());
                System.out.println("Filtro CPF: " + resultado.size() + " registros encontrados");
                break;
            case "cnpj":
                resultado = pessoas.stream()
                        .filter(p -> p.getNumeroCNPJ() != null && !p.getNumeroCNPJ().trim().isEmpty())
                        .collect(Collectors.toList());
                System.out.println("Filtro CNPJ: " + resultado.size() + " registros encontrados");
                break;
            default:
                resultado = new ArrayList<>(pessoas);
                System.out.println("Todas as pessoas: " + resultado.size() + " registros encontrados");
                break;
        }

        return resultado;
    }

    private String getTituloAba() {
        switch (abaAtiva) {
            case "cpf": return "Pessoas Físicas (CPF)";
            case "cnpj": return "Pessoas Jurídicas (CNPJ)";
            default: return "Todas as Pessoas";
        }
    }

    // Métodos específicos para cada aba
    public void exportarPdfCPF() {
        this.abaAtiva = "cpf";
        exportarPdf();
    }

    public void exportarExcelCPF() {
        this.abaAtiva = "cpf";
        exportarExcel();
    }

    public void exportarPdfCNPJ() {
        this.abaAtiva = "cnpj";
        exportarPdf();
    }

    public void exportarExcelCNPJ() {
        this.abaAtiva = "cnpj";
        exportarExcel();
    }

    public void exportarPdfTodas() {
        this.abaAtiva = "todas";
        exportarPdf();
    }

    public void exportarExcelTodas() {
        this.abaAtiva = "todas";
        exportarExcel();
    }

    // Getters e Setters
    public String getAbaAtiva() {
        return abaAtiva;
    }

    public void setAbaAtiva(String abaAtiva) {
        this.abaAtiva = abaAtiva;
    }

    public List<Pessoa> getPessoas() {
        return pessoas;
    }

    public void setPessoas(List<Pessoa> pessoas) {
        this.pessoas = pessoas;
    }

    public Pessoa getPessoaSelecionada() {
        return pessoaSelecionada;
    }

    public void setPessoaSelecionada(Pessoa pessoaSelecionada) {
        this.pessoaSelecionada = pessoaSelecionada;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getPessoaId() {
        return pessoaId;
    }

    public void setPessoaId(Long pessoaId) {
        this.pessoaId = pessoaId;
    }

    public PessoaService getPessoaService() {
        return pessoaService;
    }

    public void setPessoaService(PessoaService pessoaService) {
        this.pessoaService = pessoaService;
    }

    public Boolean getTpManutencao() {
        return tpManutencao;
    }

    public void setTpManutencao(Boolean tpManutencao) {
        this.tpManutencao = tpManutencao;
    }
}