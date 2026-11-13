package com.mantkowicz.light.architecture;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.metrics.ArchitectureMetrics;
import com.tngtech.archunit.library.metrics.ComponentDependencyMetrics;
import com.tngtech.archunit.library.metrics.LakosMetrics;
import com.tngtech.archunit.library.metrics.MetricsComponents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

/**
 * The game code's intended architecture (docs/code-quality-plan.md): layers that only use the layers below them, no
 * two packages of one layer using each other, and a few conventions.
 * <p>
 * Layers, from the bottom: <b>leaves</b> (util, debug, resources, stage: use nothing of the game), the <b>world</b>
 * (board, actor, light, event, context: what the game objects get from the game screen), <b>behaviour</b> (plugin,
 * notification), the <b>characters</b> (character: the player, the dog, the cow, the chest...; map: creates them), the
 * <b>UI</b> (ui and its subpackages), the <b>application</b> (feature, game).
 * <p>
 * The code doesn't follow all of it yet: the layer and same-layer rules are <i>frozen</i>. The violations the code had
 * when the rules came are listed in {@code e2e/architecture/frozen/} (one class-to-class dependency each) and pass; any
 * other one fails. A refactoring that removes listed violations makes the test delete them from the files, and CI
 * fails until that change is committed, so the list only shrinks. {@code -Darchitecture.update=true} records the
 * current violations instead (only for a deliberate exception, visible in the pull request).
 * <p>
 * Dependencies are read from the compiled classes, so a class using only constants of another (copied in by the
 * compiler) doesn't count.
 */
class ArchitectureTest {
    private static final String GAME = "com.mantkowicz.light";
    private static final Path ROOT = Path.of(System.getProperty("repo.root", "../..")).toAbsolutePath().normalize();
    private static final Path FROZEN = ROOT.resolve("e2e/architecture/frozen");
    private static final Path METRICS = ROOT.resolve("e2e/build/reports/architecture/metrics.md");

    private static JavaClasses game;

    @BeforeAll
    static void importGame() {
        boolean update = Boolean.getBoolean("architecture.update");
        ArchConfiguration configuration = ArchConfiguration.get();
        configuration.setProperty("freeze.store.default.path", FROZEN.toString());
        configuration.setProperty("freeze.store.default.allowStoreCreation", String.valueOf(update));
        configuration.setProperty("freeze.store.default.allowStoreUpdate", "true"); // removes fixed violations
        configuration.setProperty("freeze.refreeze", String.valueOf(update)); // adds violations only on purpose
        game = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(GAME);
    }

    @Test
    void layersUseOnlyTheLayersBelowThem() {
        ArchRule layers = layeredArchitecture().consideringOnlyDependenciesInLayers()
                .layer("Leaves").definedBy(packages("util", "debug", "resources", "stage"))
                .layer("World").definedBy(packages("board", "actor", "light", "event", "context"))
                .layer("Behaviour").definedBy(packages("plugin", "notification"))
                .layer("Characters").definedBy(packages("character", "map"))
                .layer("UI").definedBy(packages("ui"))
                .layer("Application").definedBy(packages("feature", "game"))
                .whereLayer("Application").mayNotBeAccessedByAnyLayer()
                .whereLayer("UI").mayOnlyBeAccessedByLayers("Application")
                .whereLayer("Characters").mayOnlyBeAccessedByLayers("UI", "Application")
                .whereLayer("Behaviour").mayOnlyBeAccessedByLayers("Characters", "UI", "Application")
                .whereLayer("World").mayOnlyBeAccessedByLayers("Behaviour", "Characters", "UI", "Application")
                .as("Layers use only the layers below them (leaves, world, behaviour, UI, application)");
        freeze(layers).check(game);
    }

    /** The layer rule allows two packages of one layer to use each other; these pairs must not (no cycles). */
    @Test
    void theBoardDoesNotKnowActors() {
        noClasses().that().resideInAPackage(GAME + ".board..")
                .should().dependOnClassesThat().resideInAPackage(GAME + ".actor..")
                .as("The board does not know actors (actors stand on the board)").check(game);
    }

    /** The context holds the board's service, so the board must not use the context (no cycle). */
    @Test
    void theBoardDoesNotUseTheContext() {
        noClasses().that().resideInAPackage(GAME + ".board..")
                .should().dependOnClassesThat().resideInAPackage(GAME + ".context..")
                .because("the context holds the board's service: the board takes what it needs directly")
                .check(game);
    }

    @Test
    void theEventBusUsesNothingOfTheGame() {
        noClasses().that().resideInAPackage(GAME + ".event..")
                .should()
                .dependOnClassesThat(resideInAPackage(GAME + "..").and(not(resideInAPackage(GAME + ".event.."))))
                .as("The event bus uses nothing of the game (events belong with the code they are about)")
                .check(game);
    }

    @Test
    void pluginsDoNotKnowCharacters() {
        noClasses().that().resideInAPackage(GAME + ".plugin..")
                .should().dependOnClassesThat().resideInAPackage(GAME + ".character..")
                .because("characters put plugins together; a plugin works on any actor").check(game);
    }

    @Test
    void charactersDoNotKnowTheMap() {
        noClasses().that().resideInAPackage(GAME + ".character..")
                .should().dependOnClassesThat().resideInAPackage(GAME + ".map..")
                .because("the map creates characters").check(game);
    }

    @Test
    void basicUiDoesNotKnowItsSubpackages() {
        freeze(noClasses().that().resideInAPackage(GAME + ".ui")
                .should().dependOnClassesThat().resideInAnyPackage(GAME + ".ui.menu..", GAME + ".ui.hud..",
                        GAME + ".ui.collect..")
                .as("The basic UI (stages, widgets, style) does not know the menu, the HUD or collecting"))
                .check(game);
    }

    @Test
    void featuresDoNotKnowTheGameScreen() {
        noClasses().that().resideInAPackage(GAME + ".feature..")
                .should().dependOnClassesThat().resideInAPackage(GAME + ".game..")
                .as("Features do not know the game package (it creates them)").check(game);
    }

    @Test
    void namesSayWhatClassesAre() {
        classes().that().haveSimpleNameEndingWith("Event").should().beAssignableTo(GAME + ".event.GameEvent")
                .check(game);
        classes().that().haveSimpleNameEndingWith("Plugin").should().beAssignableTo(GAME + ".actor.Plugin")
                .check(game);
        classes().that().haveSimpleNameEndingWith("Feature").should().beAssignableTo(GAME + ".feature.Feature")
                .check(game);
    }

    /** The browser build (GWT) has no reflection, no threads' concurrency tools and no String.format. */
    @Test
    void theGameRunsInTheBrowser() {
        noClasses().should().dependOnClassesThat().resideInAnyPackage("java.lang.reflect..", "java.util.concurrent..")
                .orShould().callMethod(String.class, "format", String.class, Object[].class)
                .because("the HTML5 build (GWT) can't run them")
                .check(game);
    }

    /** The rule of e2e/README.md: scenario tests speak to the game only through GameDriver. */
    @Test
    void scenarioTestsUseOnlyTheGameDriver() {
        JavaClasses scenarios = new ClassFileImporter().importPackages(GAME + ".e2e.scenario");
        noClasses().that().resideInAPackage(GAME + ".e2e.scenario..")
                .should().dependOnClassesThat(resideInAPackage(GAME + "..").and(not(resideInAPackage(GAME + ".e2e.."))))
                .because("only GameDriver touches game classes (e2e/README.md)")
                .check(scenarios);
    }

    /**
     * Not a gate: writes how tangled the packages are to e2e/build/reports/architecture/metrics.md (CI shows it on the
     * run's summary page), to watch the trend.
     */
    @Test
    void writeMetrics() throws IOException {
        Set<JavaPackage> packages = game.getPackage(GAME).getSubpackages();
        MetricsComponents<JavaClass> components = MetricsComponents.fromPackages(packages);
        LakosMetrics lakos = ArchitectureMetrics.lakosMetrics(components);
        ComponentDependencyMetrics martin = ArchitectureMetrics.componentDependencyMetrics(components);
        StringBuilder report = new StringBuilder("### Architecture metrics\n\n");
        report.append(String.format(Locale.ROOT,
                "Lakos over %d packages: RACD %.3f (share of the packages one depends on, directly or not), NCCD %.2f"
                        + " (about 1: a balanced tree; more: more tangled).%n%n",
                packages.size(), lakos.getRelativeAverageComponentDependency(),
                lakos.getNormalizedCumulativeComponentDependency()));
        report.append("| Package | Uses (Ce) | Used by (Ca) | Instability | Abstractness |\n|---|---|---|---|---|\n");
        Set<String> names = new TreeSet<>();
        packages.forEach(p -> names.add(p.getName()));
        for (String name : names) {
            report.append(String.format(Locale.ROOT, "| %s | %d | %d | %.2f | %.2f |%n",
                    name.substring(GAME.length() + 1),
                    martin.getEfferentCoupling(name), martin.getAfferentCoupling(name), martin.getInstability(name),
                    martin.getAbstractness(name)));
        }
        Files.createDirectories(METRICS.getParent());
        Files.writeString(METRICS, report);
    }

    /** "com.mantkowicz.light.board..", ... for package names under the game's root package. */
    private static String[] packages(String... names) {
        String[] patterns = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            patterns[i] = GAME + "." + names[i] + "..";
        }
        return patterns;
    }
}
