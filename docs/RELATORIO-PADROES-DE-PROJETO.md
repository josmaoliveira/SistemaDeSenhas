# Relatório de refatoração com padrões de projeto

> Relatório escrito na refatoração original do projeto (antes das etapas de multiempresa e login).
> Os trechos de código mostram como o código era naquele momento; as classes continuam existindo,
> agora no pacote `br.com.sistemadesenhas`. A estratégia de contas do Firebase (`GerenciadorContas`)
> foi adicionada depois, também com o padrão Strategy (veja `PADROES.md`).

Esta refatoração foi feita com base em problemas reais do código, e não apenas para cumprir a exigência acadêmica. A ideia foi separar responsabilidades, reduzir acoplamento e deixar o comportamento do sistema mais fácil de evoluir e testar.

## 1. Padrão Criacional: Factory

### Problema antes da refatoração

A criação de `Senha` estava misturada com a regra de negócio em `SenhaService`. O serviço calculava o código da senha e também montava o objeto inteiro, acumulando responsabilidades e dificultando manutenção.

### Padrão escolhido

`Factory`.

### Por que esse padrão era adequado

Porque a criação de `Senha` passou a ter um ponto centralizado. Se a estrutura do objeto mudar no futuro, a alteração fica concentrada em uma única classe.

### Trecho antes

```java
String prefixo = servico.getSigla().substring(0, 2).toUpperCase();
long totalParaServico = senhaRepository.countByServicoId(servicoId);
String sequencia = String.format("%03d", totalParaServico + 1);

String codigoGerado = prefixo + sequencia;

Senha novaSenha = new Senha();
novaSenha.setServico(servico);
novaSenha.setPrioridade(prioridade);
novaSenha.setDataHoraGeracao(LocalDateTime.now());
novaSenha.setStatus(StatusSenha.AGUARDANDO);
novaSenha.setCodigo(codigoGerado);

return senhaRepository.save(novaSenha);
```

### Trecho depois

```java
String prefixo = servico.getSigla().substring(0, 2).toUpperCase();
long totalParaServico = senhaRepository.countByServicoId(servicoId);
String sequencia = String.format("%03d", totalParaServico + 1);

String codigoGerado = prefixo + sequencia;

Senha novaSenha = senhaFactory.criarSenha(servico, prioridade, codigoGerado);

return senhaRepository.save(novaSenha);
```

### Código novo aplicado

```java
public Senha criarSenha(Servico servico, Prioridade prioridade, String codigo) {
	Senha nova = new Senha();
	nova.setServico(servico);
	nova.setPrioridade(prioridade);
	nova.setDataHoraGeracao(LocalDateTime.now());
	nova.setStatus(StatusSenha.AGUARDANDO);
	nova.setCodigo(codigo);
	return nova;
}
```

### Vantagens

- Separa criação de objeto da regra de negócio.
- Facilita testes unitários.
- Simplifica futuras alterações na criação da senha.

### Desvantagens

- Introduz uma classe a mais.
- Pode parecer exagero em cenários muito simples.

## 2. Padrão Estrutural: Facade/Adapter para impressão

### Problema antes da refatoração

`ImpressaoService` conhecia detalhes da biblioteca JasperReports. Isso criava acoplamento direto com uma API externa e deixava o serviço menos flexível.

### Padrão escolhido

`Facade` com uma interface de adaptação para o Jasper.

### Por que esse padrão era adequado

Porque o restante do sistema passou a falar apenas com uma abstração simples (`ReportPrinter`), sem depender dos detalhes do JasperReports.

### Trecho antes

```java
InputStream reportStream = getClass().getResourceAsStream("/reports/senha.jasper");

Map<String, Object> parametros = new HashMap<>();
parametros.put("CODIGO", senha.getCodigo());
parametros.put("SERVICO", senha.getServico().getNome());

JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Collections.singletonList(senha));

JasperPrint jasperPrint = JasperFillManager.fillReport(reportStream, parametros, dataSource);
JasperPrintManager.printReport(jasperPrint, false);
```

### Trecho depois

```java
@Autowired
private ReportPrinter reportPrinter;

public void imprimirSenhaSilenciosa(Senha senha) {
	reportPrinter.printSenha(senha);
}
```

### Código novo aplicado

```java
public interface ReportPrinter {
	void printSenha(Senha senha);
}
```

```java
@Service
public class JasperReportPrinter implements ReportPrinter {

	@Override
	public void printSenha(Senha senha) {
		// integração com JasperReports isolada aqui
	}
}
```

### Vantagens

- Reduz acoplamento com a biblioteca de relatório.
- Facilita testes com mock da interface.
- Permite trocar o mecanismo de impressão sem mexer no serviço de negócio.

### Desvantagens

- Adiciona uma camada extra de abstração.
- Exige decidir qual implementação será injetada.

## 3. Padrão Comportamental: Strategy

### Problema antes da refatoração

A forma de escolher a próxima senha estava embutida em `AtendimentoService`, o que torna difícil mudar a política de seleção no futuro (por exemplo, FIFO, prioridade por peso, regras por guichê etc.).

### Padrão escolhido

`Strategy`.

### Por que esse padrão era adequado

Porque a seleção da próxima senha é um comportamento que pode variar. Com Strategy, o algoritmo fica isolado e pode ser substituído sem alterar a classe principal do atendimento.

### Trecho antes

```java
List<Senha> fila = senhaRepository.encontrarProximaSenha(guiche.getServico());
```

### Trecho depois

```java
@Autowired
private NextSenhaStrategy nextSenhaStrategy;

List<Senha> fila = nextSenhaStrategy.encontrarProxima(guiche.getServico());
```

### Código novo aplicado

```java
public interface NextSenhaStrategy {
	List<Senha> encontrarProxima(Servico servico);
}
```

```java
@Service
public class PrioridadeStrategy implements NextSenhaStrategy {

	@Autowired
	private SenhaRepository senhaRepository;

	@Override
	public List<Senha> encontrarProxima(Servico servico) {
		return senhaRepository.encontrarProximaSenha(servico);
	}
}
```

### Vantagens

- Permite trocar a política de seleção com baixo impacto.
- Torna o comportamento mais explícito.
- Facilita testes de algoritmos diferentes.

### Desvantagens

- Mais classes no projeto.
- Se houver muitas estratégias, será preciso gerenciar melhor a seleção da implementação.

## Conclusão

Os três padrões foram aplicados em pontos onde havia problema real de design:

- `Factory` para organizar criação de objetos.
- `Facade/Adapter` para encapsular a biblioteca de impressão.
- `Strategy` para permitir variação no algoritmo de seleção da próxima senha.

Com isso, o código ficou mais modular, mais fácil de testar e mais preparado para mudanças futuras.
