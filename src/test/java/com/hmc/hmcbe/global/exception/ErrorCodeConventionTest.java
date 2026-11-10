package com.hmc.hmcbe.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErrorCodeConventionTest {

	private static final String BASE_PACKAGE = "com.hmc.hmcbe";
	private static final Pattern CODE_FORMAT = Pattern.compile("^([A-Z]+)(\\d{3})([1-9])$");

	@Test
	@DisplayName("모든 에러 코드는 {DOMAIN}{HTTP 상태}{순번} 형식이고, 도메인 접두사·상태가 실제와 일치하며 서로 겹치지 않는다")
	void errorCodesFollowConvention() {
		List<String> violations = new ArrayList<>();
		Set<String> seen = new HashSet<>();

		for (JavaClass javaClass : errorCodeEnums()) {
			String domain = domainOf(javaClass.getPackageName());
			String expectedPrefix = domain.equals("global") ? "COMMON" : domain.toUpperCase(Locale.ROOT);
			if (!domain.equals("global") && !javaClass.getPackageName().equals(BASE_PACKAGE + "." + domain + ".exception")) {
				violations.add(javaClass.getName() + ": 도메인 에러 코드는 {domain}.exception 패키지에 둔다");
			}
			for (Object constant : javaClass.reflect().getEnumConstants()) {
				ErrorCode errorCode = (ErrorCode) constant;
				String where = javaClass.getSimpleName() + "." + constant;
				Matcher matcher = CODE_FORMAT.matcher(errorCode.getCode());
				if (!matcher.matches()) {
					violations.add(where + ": 형식 위반 " + errorCode.getCode());
					continue;
				}
				if (!matcher.group(1).equals(expectedPrefix)) {
					violations.add(where + ": 접두사는 " + expectedPrefix + "여야 한다 (" + errorCode.getCode() + ")");
				}
				if (Integer.parseInt(matcher.group(2)) != errorCode.getStatus().value()) {
					violations.add(where + ": 코드의 상태(" + matcher.group(2) + ")와 HttpStatus("
							+ errorCode.getStatus().value() + ")가 다르다");
				}
				if (!seen.add(errorCode.getCode())) {
					violations.add(where + ": 중복 코드 " + errorCode.getCode());
				}
			}
		}

		assertThat(violations).isEmpty();
	}

	private static List<JavaClass> errorCodeEnums() {
		List<JavaClass> enums = new ClassFileImporter()
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages(BASE_PACKAGE).stream()
				.filter(JavaClass::isEnum)
				.filter(c -> c.isAssignableTo(ErrorCode.class))
				.toList();
		assertThat(enums).isNotEmpty();
		return enums;
	}

	private static String domainOf(String packageName) {
		return packageName.substring(BASE_PACKAGE.length() + 1).split("\\.")[0];
	}
}
