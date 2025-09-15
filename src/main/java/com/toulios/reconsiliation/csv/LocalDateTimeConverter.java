package com.toulios.reconsiliation.csv;

import com.opencsv.bean.AbstractBeanField;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * OpenCSV converter for parsing CSV date-time values into {@link LocalDateTime}.
 *
 * <p>Expected format: {@code yyyy-MM-dd HH:mm:ss} as per the sample CSV.
 * Null and blank values are returned as {@code null}. Invalid formats result
 * in an {@link IllegalArgumentException} with a descriptive message.</p>
 */
public class LocalDateTimeConverter extends AbstractBeanField<LocalDateTime, String> {

	private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * Converts a CSV cell string to {@link LocalDateTime} using the expected pattern.
	 *
	 * @param value the raw CSV cell value
	 * @return parsed {@link LocalDateTime} or {@code null}
	 */
	@Override
	protected LocalDateTime convert(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		try {
			return LocalDateTime.parse(trimmed, DEFAULT_FORMATTER);
		} catch (DateTimeParseException ex) {
			throw new IllegalArgumentException("Invalid date-time format: '" + value + "' expected yyyy-MM-dd HH:mm:ss", ex);
		}
	}
}


