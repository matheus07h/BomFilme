package br.ufrn.bomfilme.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * A regra de dependência, como teste. O ArchUnit lê o bytecode compilado:
 *
 * <pre>
 *     adaptadores (http, banco, tmdb)  ──►  aplicacao  ──►  dominio
 * </pre>
 *
 * <p>A seta só aponta para dentro. Quem liga as peças é o CDI, então não há raiz de
 * composição escrita à mão para conferir.
 */
class ArquiteturaTest {

    static final ArchRule REGRA_DO_DOMINIO = noClasses()
            .that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta..", "io.quarkus..", "org.hibernate..", "com.fasterxml..",
                    "java.sql..", "org.eclipse.microprofile..",
                    "..adaptadores..", "..aplicacao..")
            .because("o domínio é Java puro: não sabe de HTTP, banco, serialização nem injeção");

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("br.ufrn.bomfilme");

    @Test
    void oDominioNaoConheceFrameworkInfraestruturaNemAsCamadasDeFora() {
        REGRA_DO_DOMINIO.check(classes);
    }

    @Test
    void osCasosDeUsoNaoConhecemOsAdaptadores() {
        noClasses().that().resideInAPackage("..aplicacao..")
                .should().dependOnClassesThat().resideInAnyPackage("..adaptadores..")
                .because("o caso de uso fala com as portas do domínio, não com quem as implementa")
                .check(classes);
    }

    @Test
    void oAdaptadorHttpNaoFalaComOBanco() {
        noClasses().that().resideInAPackage("..adaptadores.http..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adaptadores.banco..", "jakarta.persistence..", "io.quarkus.hibernate..")
                .because("o recurso não acessa persistência: isso é trabalho do adaptador de banco")
                .check(classes);
    }

    @Test
    void oAdaptadorDeBancoNaoFalaComHttpNemComOTMDB() {
        noClasses().that().resideInAPackage("..adaptadores.banco..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adaptadores.http..", "..adaptadores.tmdb..", "jakarta.ws.rs..")
                .because("adaptadores são irmãos: nenhum depende do outro")
                .check(classes);
    }

    /**
     * A regra falha quando deve? {@code violacao.dominio.Contaminado}, que só existe nos
     * testes, importa Jakarta de propósito. Se um dia a regra passar com ela, deixou de
     * proteger qualquer coisa.
     */
    @Test
    void aRegraDoDominioPegaUmaViolacao() {
        JavaClasses fixture = new ClassFileImporter()
                .importPackages("br.ufrn.bomfilme.arquitetura.violacao");
        assertTrue(REGRA_DO_DOMINIO.evaluate(fixture).hasViolation(),
                "a regra deveria acusar a classe contaminada de propósito");
    }
}
