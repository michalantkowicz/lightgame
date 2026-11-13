package com.mantkowicz.light.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.metrics.ArchitectureMetrics;
import com.tngtech.archunit.library.metrics.ComponentDependencyMetrics;
import com.tngtech.archunit.library.metrics.LakosMetrics;
import com.tngtech.archunit.library.metrics.MetricsComponents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Keeps the game code from getting more tangled (docs/code-quality-plan.md, phase 2). The game's packages depend on
 * each other only as listed in {@code e2e/architecture/package-dependencies.txt}: a new dependency fails here and must
 * either go or be added to the list on purpose, where the pull request shows it; a listed one that is gone fails too,
 * so the list only ever shrinks by itself. {@code -Darchitecture.update=true} rewrites the list from the code.
 * <p>
 * Dependencies are read from the compiled classes, so a class that uses only constants of another (copied in by the
 * compiler) doesn't count.
 */
class ArchitectureTest {
    private static final String GAME = "com.mantkowicz.light";
    private static final Path ROOT = Path.of(System.getProperty("repo.root", "../..")).toAbsolutePath().normalize();
    private static final Path ALLOWED = ROOT.resolve("e2e/architecture/package-dependencies.txt");
    private static final Path METRICS = ROOT.resolve("e2e/build/reports/architecture/metrics.md");
    /** Packages that depend on no other game package: everything may use them, they use nothing of the game. */
    private static final List<String> LEAVES = List.of("debug", "resources", "stage", "util");

    private static JavaClasses game;

    @BeforeAll
    static void importGame() {
        game = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(GAME);
    }

    @Test
    void packagesDependOnEachOtherOnlyAsListed() throws IOException {
        Map<String, String> found = packageDependencies();
        if (Boolean.getBoolean("architecture.update")) {
            write(found.keySet());
            return;
        }
        Set<String> allowed = read();
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, String> dependency : found.entrySet()) {
            if (!allowed.contains(dependency.getKey())) {
                problems.add("new dependency " + dependency.getKey() + " (e.g. " + dependency.getValue()
                        + "): remove it, or add the line to the list if it is meant");
            }
        }
        for (String line : allowed) {
            if (!found.containsKey(line)) {
                problems.add("no longer a dependency: " + line + " (nice): delete the line from the list");
            }
        }
        if (!problems.isEmpty()) {
            fail("Package dependencies differ from " + ROOT.relativize(ALLOWED) + ":\n  "
                    + String.join("\n  ", problems)
                    + "\n(-Darchitecture.update=true rewrites the list from the code)");
        }
    }

    @Test
    void leafPackagesUseNoOtherGamePackage() {
        for (String leaf : LEAVES) {
            noClasses().that().resideInAPackage(GAME + "." + leaf + "..")
                    .should().dependOnClassesThat(resideInAPackage(GAME + "..")
                            .and(not(resideInAPackage(GAME + "." + leaf + ".."))))
                    .because(leaf + " is a leaf: the rest of the game uses it, it uses nothing of the game")
                    .check(game);
        }
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
                "%d package dependencies. Lakos over %d packages: RACD %.3f (share of the packages one depends on, directly"
                        + " or not), NCCD %.2f (about 1: a balanced tree; more: more tangled).%n%n",
                packageDependencies().size(), packages.size(), lakos.getRelativeAverageComponentDependency(),
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

    /** "from -> to" (package names under com.mantkowicz.light) with one example "Class -> Class" each. */
    private static Map<String, String> packageDependencies() {
        Map<String, String> dependencies = new TreeMap<>();
        for (JavaClass origin : game) {
            for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                JavaClass target = dependency.getTargetClass();
                String from = origin.getPackageName(), to = target.getPackageName();
                if (to.startsWith(GAME + ".") && !from.equals(to)) {
                    dependencies.putIfAbsent(relative(from) + " -> " + relative(to),
                            origin.getSimpleName() + " -> " + target.getSimpleName());
                }
            }
        }
        return dependencies;
    }

    private static String relative(String packageName) {
        return packageName.substring(GAME.length() + 1);
    }

    private static Set<String> read() throws IOException {
        Set<String> lines = new TreeSet<>();
        for (String line : Files.readAllLines(ALLOWED)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                lines.add(line.trim());
            }
        }
        return lines;
    }

    private static void write(Set<String> dependencies) throws IOException {
        List<String> lines = new ArrayList<>(List.of(
                "# Which game packages (under com.mantkowicz.light) may use which: ArchitectureTest fails on any other.",
                "# A new line here is a new tangle: add one only on purpose. Remove lines when a refactoring makes them",
                "# untrue (the test says so). -Darchitecture.update=true rewrites this file from the code.",
                ""));
        lines.addAll(dependencies);
        Files.write(ALLOWED, lines);
    }
}
