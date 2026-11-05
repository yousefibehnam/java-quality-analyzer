package io.github.yousefibehnam.jqa.report;

import io.github.yousefibehnam.jqa.metrics.ClassMetrics;
import io.github.yousefibehnam.jqa.model.TypeKind;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.OptionalDouble;

import static org.assertj.core.api.Assertions.assertThat;

class ReportWriterTest {

    private static final List<ClassMetrics> ROWS = List.of(
            new ClassMetrics("a.B", TypeKind.CLASS, "a/B.java", 3, 2, 5, 1, true, 0, 2, 7, 1, OptionalDouble.of(0.5)),
            new ClassMetrics("a.C", TypeKind.INTERFACE, "dir,with \"comma\"/C.java", 1, 0, 1, 1, false, 2, 0, 1, 0,
                    OptionalDouble.empty()));

    private static String render(ReportWriter writer) throws IOException {
        StringWriter out = new StringWriter();
        writer.write(ROWS, out);
        return out.toString();
    }

    @Test
    void csvHasHeaderEscapingAndEmptyUndefinedValues() throws IOException {
        assertThat(render(new CsvReportWriter())).isEqualTo("""
                class,kind,file,methods,fields,wmc,dit,dit_exact,noc,cbo,rfc,lcom,lcom_star
                a.B,class,a/B.java,3,2,5,1,true,0,2,7,1,0.5000
                a.C,interface,"dir,with ""comma""/C.java",1,0,1,1,false,2,0,1,0,
                """);
    }

    @Test
    void jsonUsesNullForUndefinedValues() throws IOException {
        String json = render(new JsonReportWriter());
        assertThat(json).startsWith("[\n  {\"class\": \"a.B\"")
                .contains("\"lcom_star\": 0.5000")
                .contains("\"file\": \"dir,with \\\"comma\\\"/C.java\"")
                .contains("\"lcom_star\": null")
                .endsWith("}\n]\n");
    }

    @Test
    void emptyJsonIsAnEmptyArray() throws IOException {
        StringWriter out = new StringWriter();
        new JsonReportWriter().write(List.of(), out);
        assertThat(out).hasToString("[]\n");
    }

    @Test
    void jsonEscapesControlCharacters() {
        assertThat(JsonReportWriter.quote("a\tb\u0001")).isEqualTo("\"a\\tb\\u0001\"");
    }
}
