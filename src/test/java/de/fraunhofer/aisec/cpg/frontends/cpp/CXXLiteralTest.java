/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */

package de.fraunhofer.aisec.cpg.frontends.cpp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.Literal;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.Type;
import de.fraunhofer.aisec.cpg.graph.UnaryOperator;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import java.io.File;
import java.math.BigInteger;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class CXXLiteralTest {

  private TranslationConfiguration config;

  @BeforeEach
  void setUp() {
    config = TranslationConfiguration.builder().defaultPasses().build();
  }

  @Test
  void testDecimalIntegerLiterals() throws TranslationException {
    TranslationUnitDeclaration tu =
        new CXXLanguageFrontend(config).parse(new File("src/test/resources/integer_literals.cpp"));

    FunctionDeclaration decimal =
        tu.getDeclarationByName("decimal", FunctionDeclaration.class).orElse(null);
    assertNotNull(decimal);
    assertEquals("decimal", decimal.getName());

    assertLiteral(42, CXXLanguageFrontend.INT_TYPE, decimal, "i");
    assertLiteral(9223372036854775807L, CXXLanguageFrontend.LONG_TYPE, decimal, "l");
    assertLiteral(9223372036854775807L, CXXLanguageFrontend.LONG_TYPE, decimal, "l_with_suffix");
    assertLiteral(
        9223372036854775807L,
        CXXLanguageFrontend.LONG_LONG_TYPE,
        decimal,
        "l_long_long_with_suffix");

    assertLiteral(
        new BigInteger("9223372036854775809"),
        CXXLanguageFrontend.TYPE_UNSIGNED_LONG,
        decimal,
        "l_unsigned_long_with_suffix");
    assertLiteral(
        new BigInteger("9223372036854775808"),
        CXXLanguageFrontend.TYPE_UNSIGNED_LONG_LONG,
        decimal,
        "l_long_long_implicit");
    assertLiteral(
        new BigInteger("9223372036854775809"),
        CXXLanguageFrontend.TYPE_UNSIGNED_LONG_LONG,
        decimal,
        "l_unsigned_long_long_with_suffix");
  }

  @Test
  void testOctalIntegerLiterals() throws TranslationException {
    TranslationUnitDeclaration tu =
        new CXXLanguageFrontend(config).parse(new File("src/test/resources/integer_literals.cpp"));

    FunctionDeclaration octal =
        tu.getDeclarationByName("octal", FunctionDeclaration.class).orElse(null);
    assertNotNull(octal);
    assertEquals("octal", octal.getName());

    assertLiteral(42, CXXLanguageFrontend.INT_TYPE, octal, "i");
    assertLiteral(42L, CXXLanguageFrontend.LONG_TYPE, octal, "l_with_suffix");
    assertLiteral(
        BigInteger.valueOf(42),
        CXXLanguageFrontend.TYPE_UNSIGNED_LONG_LONG,
        octal,
        "l_unsigned_long_long_with_suffix");
  }

  @ParameterizedTest
  @ValueSource(strings = {"octal", "hex", "binary"})
  void testNonDecimalIntegerLiterals() throws TranslationException {
    TranslationUnitDeclaration tu =
        new CXXLanguageFrontend(config).parse(new File("src/test/resources/integer_literals.cpp"));

    FunctionDeclaration functionDeclaration =
        tu.getDeclarationByName("hex", FunctionDeclaration.class).orElse(null);
    assertNotNull(functionDeclaration);
    assertEquals("hex", functionDeclaration.getName());

    assertLiteral(42, CXXLanguageFrontend.INT_TYPE, functionDeclaration, "i");
    assertLiteral(42L, CXXLanguageFrontend.LONG_TYPE, functionDeclaration, "l_with_suffix");
    assertLiteral(
        BigInteger.valueOf(42),
        CXXLanguageFrontend.TYPE_UNSIGNED_LONG_LONG,
        functionDeclaration,
        "l_unsigned_long_long_with_suffix");
  }

  @Test
  void testLargeNegativeNumber() throws TranslationException {
    TranslationUnitDeclaration tu =
        new CXXLanguageFrontend(config)
            .parse(new File("src/test/resources/largenegativenumber.cpp"));

    FunctionDeclaration main =
        tu.getDeclarationByName("main", FunctionDeclaration.class).orElse(null);
    assertNotNull(main);

    VariableDeclaration a = main.getVariableDeclarationByName("a").orElse(null);
    assertNotNull(a);
    assertEquals(
        1,
        ((Literal) Objects.requireNonNull(a.getInitializerAs(UnaryOperator.class)).getInput())
            .getValue());

    // there are no negative literals, so the construct "-2147483648" is
    // a unary expression and the literal "2147483648". Since "2147483648" is too large to fit
    // in an integer, it should be automatically converted to a long. The resulting value
    // -2147483648 however is small enough to fit into an int, so it is ok for the variable a to
    // have an int type
    VariableDeclaration b = main.getVariableDeclarationByName("b").orElse(null);
    assertNotNull(b);
    assertEquals(
        2147483648L,
        ((Literal) Objects.requireNonNull(b.getInitializerAs(UnaryOperator.class)).getInput())
            .getValue());

    VariableDeclaration c = main.getVariableDeclarationByName("c").orElse(null);
    assertNotNull(c);
    assertEquals(
        2147483649L,
        ((Literal) Objects.requireNonNull(c.getInitializerAs(UnaryOperator.class)).getInput())
            .getValue());

    VariableDeclaration d = main.getVariableDeclarationByName("d").orElse(null);
    assertNotNull(d);
    assertEquals(
        new BigInteger("9223372036854775808"),
        ((Literal) Objects.requireNonNull(d.getInitializerAs(UnaryOperator.class)).getInput())
            .getValue());
  }

  private void assertLiteral(
      Number expectedValue,
      Type expectedType,
      FunctionDeclaration functionDeclaration,
      String name) {
    VariableDeclaration variableDeclaration =
        functionDeclaration.getVariableDeclarationByName(name).orElse(null);
    assertNotNull(variableDeclaration);

    Literal literal = variableDeclaration.getInitializerAs(Literal.class);
    assertNotNull(literal);

    assertEquals(expectedType, literal.getType());
    assertEquals(expectedValue, literal.getValue());
  }
}