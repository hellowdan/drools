package org.kie.scanner.regression;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.appformer.maven.integration.MavenRepository;

import org.assertj.core.api.Assertions;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.KieScanner;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

public class KieScannerTest {

    private KieServices kieServices;
    private MavenRepository repository;

    public static synchronized File getTempDir() {
        File tempDir;
        try {
            tempDir = new File("target/tmp");
        } catch (Exception ex) {
            tempDir = new File(System.getProperty("java.io.tmpdir"), "brms-tests");
        }

        if (!tempDir.exists() && !tempDir.mkdir()) {
            throw new IllegalStateException("Cannot create temp.dir at '" + tempDir.getAbsolutePath() + "'!");
        }

        return tempDir;
    }

    private File createTmpTextFile(String fileName, String text) throws IOException {

        File file = new File(getTempDir(), fileName);

        BufferedWriter output = new BufferedWriter(new FileWriter(file));

        output.write(text);

        output.close();

        return file;
    }

    private File getPomFile(ReleaseId releaseId) throws IOException {
        return createTmpTextFile("pom.xml", getPomXml(releaseId));
    }

    private String getPomXml(ReleaseId releaseId) {
        return this.getPomXml(releaseId, false);
    }

    private String getPomXml(ReleaseId releaseId, boolean packaging, ReleaseId... dependency) {
        String pomXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0"
                + " http://maven.apache.org/maven-v4_0_0.xsd\">\n" + "  <modelVersion>4.0.0</modelVersion>\n" + "\n"
                + "  <groupId>" + releaseId.getGroupId() + "</groupId>\n" + "  <artifactId>"
                + releaseId.getArtifactId() + "</artifactId>\n" + "  <version>" + releaseId.getVersion()
                + "</version>\n";

        if (packaging) {
            pomXml += "  <packaging>pom</packaging>";
        }

        pomXml += "\n";

        if (dependency != null) {
            pomXml += "  <dependencies>\n";

            for (ReleaseId rid : dependency) {
                pomXml += "    <dependency>\n" + "      <groupId>" + rid.getGroupId() + "</groupId>\n"
                        + "      <artifactId>" + rid.getArtifactId() + "</artifactId>\n" + "      <version>"
                        + rid.getVersion() + "</version>\n" + "    </dependency>\n";
            }

            pomXml += "  </dependencies>\n";
        }

        pomXml += "</project>";

        return pomXml;
    }

    private String getRuleDrl(String... ruleNames) {
        return getRuleDrlWithPackage("org.kie.scanner.regression", null, ruleNames);
    }

    private String getRuleDrlWithPackage(String packageName, String message, String... ruleNames) {
        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(packageName).append("\n");
        builder.append("global java.util.List list\n");

        for (String ruleName : ruleNames) {
            builder.append("rule ").append(ruleName).append("\n");
            builder.append("    when\n");
            builder.append("    then\n");
            builder.append("        list.add( drools.getRule().getName() );\n");
            if (message != null) {
                builder.append("        System.out.println(\"").append(message).append("\");\n");
            }
            builder.append("end\n");
        }

        return builder.toString();
    }

    private KieModule createKieModule(KieServices kieServices, String pomXml, String kieBaseName,
                                      String kieSessionName, String path, File... files) {

        KieFileSystem kfs = kieServices.newKieFileSystem();

        if (kieBaseName != null && kieSessionName != null) {
            KieModuleModel kieModuleModel = kieServices.newKieModuleModel();

            KieBaseModel baseModel = kieModuleModel.newKieBaseModel(kieBaseName);
            baseModel.addPackage("*").newKieSessionModel(kieSessionName);

            kfs.writeKModuleXML(kieModuleModel.toXML());
        }

        kfs.writePomXML(pomXml);

        for (File f : files) {

            if (f.getName().endsWith(".java")) {
                kfs.write("src/main/java/" + path + f.getName(), kieServices.getResources().newFileSystemResource(f));
            } else {
                kfs.write("src/main/resources/" + path + f.getName(),
                          kieServices.getResources().newFileSystemResource(f));
            }
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);

        Assertions.assertThat(kieBuilder.buildAll().getResults().getMessages().isEmpty()).isTrue();

        return kieBuilder.getKieModule();
    }

    private InternalKieModule createInternalKieModule(KieServices kieServices, String pomXml, File... files) {
        return (InternalKieModule) createKieModule(kieServices, pomXml, null, null,
                                                   "org/kie/scanner/regression/", files);
    }

    private InternalKieModule createInternalKieModule(KieServices kieServices, String pomXml, String kieBaseName,
                                                      String kieSessionName, File... files) {
        return (InternalKieModule) createKieModule(kieServices, pomXml, kieBaseName, kieSessionName,
                                                   "org/kie/scanner/regression/", files);
    }

    public static void sleepNanos(final long nanoDuration) throws InterruptedException {
        final long end = System.nanoTime() + nanoDuration;
        long timeLeft = nanoDuration;
        do {
            if (timeLeft > 50000) {
                Thread.sleep(1);
            } else if (timeLeft > 30000) {
                Thread.yield();
            }
            timeLeft = end - System.nanoTime();
        } while (timeLeft > 0);
    }

    public static byte[] getFileBytes(final File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Error during reading file " + file.getAbsolutePath(), e);
        }
    }

    private void installArtifactAndWait(ReleaseId releaseId, InternalKieModule jar, File pomFile) throws InterruptedException {
        repository.installArtifact(releaseId, jar.getBytes(), getFileBytes(pomFile));
        sleepNanos(TimeUnit.MILLISECONDS.toNanos(1000));
    }

    private void checkNewKieSession(KieContainer kieContainer, String result) {
        this.checkNewKieSession(kieContainer, result, null);
    }

    private KieSession createKieSession(KieContainer kieContainer, String kieSessionName) {
        KieSession kieSession;

        if (kieSessionName == null) {
            kieSession = kieContainer.newKieSession();
        } else {
            kieSession = kieContainer.newKieSession(kieSessionName);
        }
        return kieSession;
    }

    private void verifyKieSession(KieSession kieSession, String result) {
        List<String> list = new ArrayList<String>();
        kieSession.setGlobal("list", list);
        kieSession.fireAllRules();

        Assertions.assertThat(list.size()).isEqualTo(1);
        Assertions.assertThat(list.get(0)).isEqualTo(result);
    }

    private void checkNewKieSession(KieContainer kieContainer, String result, String kieSessionName) {
        KieSession kieSession = createKieSession(kieContainer, kieSessionName);
        try {
            verifyKieSession(kieSession, result);
        } finally {
            if (kieSession != null) {
                kieSession.dispose();
            }
        }
    }

    @Before
    public void initialize() throws IOException {

        repository = MavenRepository.getMavenRepository();

        kieServices = KieServices.Factory.get();
    }

    @Test
    public void startScanTest() throws IOException, InterruptedException {

        Long deployTime;

        ReleaseId releaseId = kieServices.newReleaseId("org.kie.scanner.regression", "scanner-start-scan-test", "1.0-SNAPSHOT");

        File pomFile = getPomFile(releaseId);
        File rule1 = createTmpTextFile("rule1.drl", getRuleDrl("rule1"));
        File rule2 = createTmpTextFile("rule2.drl", getRuleDrl("rule2"));
        File rule3 = createTmpTextFile("rule3.drl", getRuleDrl("rule3"));

        InternalKieModule iKieModule1 = createInternalKieModule(kieServices, getPomXml(releaseId), rule1);
        installArtifactAndWait(releaseId, iKieModule1, pomFile);

        KieContainer kieContainer = kieServices.newKieContainer(releaseId);
        KieScanner scanner = kieServices.newKieScanner(kieContainer);

        scanner.start(2000);
        try {
            Long zeroTime = System.currentTimeMillis();

            checkNewKieSession(kieContainer, "rule1");

            sleepAtIntervalStart(zeroTime);
            InternalKieModule iKieModule2 = createInternalKieModule(kieServices, getPomXml(releaseId), rule2);
            installArtifactAndWait(releaseId, iKieModule2, pomFile);

            scanner.scanNow();

            checkNewKieSession(kieContainer, "rule2");

            sleepAtIntervalStart(zeroTime);
            InternalKieModule iKieModule3 = createInternalKieModule(kieServices, getPomXml(releaseId), rule3);
            repository.installArtifact(releaseId, iKieModule3.getBytes(), getFileBytes(pomFile));

            deployTime = System.currentTimeMillis();

            sleepBeforeScan(deployTime, zeroTime);
            checkNewKieSession(kieContainer, "rule2");
            sleepAfterScan(deployTime, zeroTime);
            checkNewKieSession(kieContainer, "rule3");
        } finally {
            scanner.shutdown();
        }
    }

    private void sleepAtIntervalStart(Long zeroTime) throws InterruptedException {
        int intervalNumber = (int) ((System.currentTimeMillis() - zeroTime) / 2000) + 1;
        sleepNanos(TimeUnit.MILLISECONDS.toNanos((zeroTime + intervalNumber * 2000) - System.currentTimeMillis()));
    }

    private void sleepBeforeScan(Long deployTime, Long zeroTime) throws InterruptedException {
        // no sleep necessary - remove after verification
    }

    private void sleepAfterScan(Long deployTime, Long zeroTime) throws InterruptedException {
        sleepNanos(TimeUnit.MILLISECONDS.toNanos(2000 * 3 / 2));
    }

}
