// Copyright 2019 DLR - GSOC
// SPDX-License-Identifier: Apache-2.0
package de.dlr.gsoc.mcds.mosdl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Helper class for parsing command line options.
 * <p>
 * Use the builder returned by {@link #create()} to specify required, optional and toggle options.
 * You can also change some behavior of the created parser here. Build the parser using
 * {@link Builder#build()} and use the returned parser for parsing a command line by invoking
 * {@link #parse(String...)}. Only after parsing you can access the command line options using
 * various methods, e.g. {@link #get(String)}.
 */
public class CommandLineOptions {

	private final String description;
	private final Map<String, Option<?>> optionsByLongName;
	private final Map<String, Option<?>> optionsByShortName;
	private final Map<String, Option<?>> optionsByPlaceholderName;
	private final LinkedHashMap<Option<?>, Object> optionInstances = new LinkedHashMap<>();
	private final List<Option<?>> plainOptions;
	private final String longPrefix;
	private final String shortPrefix;
	private final boolean isCaseSensitiveLongOptions;
	private final boolean isCaseSensitiveShortOptions;
	private final boolean isCaseSensitiveEnumValues;
	private final boolean isAllowAbbrevEnumValues;
	private final boolean isPrintUsageOnError;
	private final boolean isExitOnHelpOrError;
	private boolean isParsed = false;

	private CommandLineOptions(String description, List<Option<?>> options, String longPrefix, String shortPrefix,
			boolean isCaseSensitiveLongOptions, boolean isCaseSensitiveShortOptions, boolean isCaseSensitiveEnumValues, boolean isAllowAbbrevEnumValues,
			boolean isPrintUsageOnError, boolean isExitOnHelpOrError) {
		this.description = description;
		this.optionsByLongName = options.stream()
				.filter(o -> null != o.getLongName() && !o.getLongName().isEmpty())
				.collect(Collectors.toMap(o -> isCaseSensitiveLongOptions ? o.getLongName() : o.getLongName().toUpperCase(), o -> o));
		this.optionsByShortName = options.stream()
				.filter(o -> null != o.getShortName() && !o.getShortName().isEmpty())
				.collect(Collectors.toMap(o -> isCaseSensitiveShortOptions ? o.getShortName() : o.getShortName().toUpperCase(), o -> o));
		this.optionsByPlaceholderName = options.stream()
				.filter(o -> null != o.getPlaceholderName() && !o.getPlaceholderName().isEmpty())
				.collect(Collectors.toMap(o -> o.getPlaceholderName(), o -> o));
		options.stream().forEach(o -> optionInstances.put(o, null));
		this.plainOptions = options.stream()
				.filter(o -> null != o.getPlaceholderName() && !o.getPlaceholderName().isEmpty())
				.filter(o -> null == o.getLongName() || o.getLongName().isEmpty())
				.filter(o -> null == o.getShortName() || o.getShortName().isEmpty())
				.collect(Collectors.toList());
		boolean haveSeenOptional = false;
		for (Option<?> opt : plainOptions) {
			if (haveSeenOptional && opt.isRequired()) {
				throw new IllegalArgumentException("Optional plain options may not be followed by required ones.");
			}
			haveSeenOptional |= !opt.isRequired();
		}
		this.longPrefix = longPrefix;
		this.shortPrefix = shortPrefix;
		this.isCaseSensitiveLongOptions = isCaseSensitiveLongOptions;
		this.isCaseSensitiveShortOptions = isCaseSensitiveShortOptions;
		this.isCaseSensitiveEnumValues = isCaseSensitiveEnumValues;
		this.isAllowAbbrevEnumValues = isAllowAbbrevEnumValues;
		this.isPrintUsageOnError = isPrintUsageOnError;
		this.isExitOnHelpOrError = isExitOnHelpOrError;
	}

	public CommandLineOptions parse(String... args) {
		try {
			return doParse(args);
		} catch (Exception ex) {
			System.err.println(ex.toString());
			System.err.println();
			if (isPrintUsageOnError) {
				printUsage();
			}
			if (isExitOnHelpOrError) {
				System.exit(-1);
			}
		}
		return this;
	}

	private CommandLineOptions doParse(String... args) {
		ParseMode mode = ParseMode.PARSE_OPTION;
		Option<?> currentOption = null;
		ListIterator<Option<?>> nextPlaceholderIter = plainOptions.listIterator();
		for (String arg : args) {
			switch (mode) {
				case PARSE_OPTION:
					if (!longPrefix.isEmpty() && arg.startsWith(longPrefix)) {
						// long option
						String longName = arg.substring(longPrefix.length());
						if (!isCaseSensitiveLongOptions) {
							longName = longName.toUpperCase();
						}
						currentOption = optionsByLongName.get(longName);
						if (null == currentOption) {
							throw new IllegalArgumentException("Unknown option '" + arg + "'.");
						}
						if (currentOption.isHelp) {
							printHelp();
							if (isExitOnHelpOrError) {
								System.exit(0);
							}
						}
						if (currentOption.getArity() == 0 && currentOption.getType().equals(Boolean.class)) {
							// binary toggle was given - maps to true
							optionInstances.put(currentOption, true);
						} else {
							mode = ParseMode.PARSE_VALUE;
						}
					} else if (!shortPrefix.isEmpty() && arg.startsWith(shortPrefix)) {
						// short option
						String shortName = arg.substring(shortPrefix.length());
						if (!isCaseSensitiveShortOptions) {
							shortName = shortName.toUpperCase();
						}
						currentOption = optionsByShortName.get(shortName);
						if (null == currentOption) {
							throw new IllegalArgumentException("Unknown option '" + arg + "'.");
						}
						if (currentOption.isHelp) {
							printHelp();
							if (isExitOnHelpOrError) {
								System.exit(0);
							}
						}
						if (currentOption.getArity() == 0 && currentOption.getType().equals(Boolean.class)) {
							// binary toggle was given - maps to true
							optionInstances.put(currentOption, true);
						} else {
							mode = ParseMode.PARSE_VALUE;
						}
					} else {
						// plain value
						if (!nextPlaceholderIter.hasNext()) {
							throw new IllegalArgumentException("Unknown value '" + arg + "'.");
						}
						currentOption = nextPlaceholderIter.next();
						optionInstances.put(currentOption, currentOption.map(arg, isCaseSensitiveEnumValues, isAllowAbbrevEnumValues));
					}
					break;
				case PARSE_VALUE:
					Object oldValue = optionInstances.putIfAbsent(currentOption, currentOption.map(arg, isCaseSensitiveEnumValues, isAllowAbbrevEnumValues));
					if (null != oldValue) {
						throw new IllegalArgumentException("Option '" + currentOption + "' was given more than once.");
					}
					mode = ParseMode.PARSE_OPTION;
					break;
			}
		}
		List<Option<?>> missingOptions = new ArrayList<>();
		for (Map.Entry<Option<?>, Object> optEntry : optionInstances.entrySet()) {
			Option<?> option = optEntry.getKey();
			Object value = optEntry.getValue();
			if (null == value && option.getArity() == 0 && option.getType().equals(Boolean.class)) {
				// binary toggle was not given - maps to false
				optEntry.setValue(false);
			} else if (null == value && option.isRequired() && null == option.getDefaultValue()) {
				missingOptions.add(option);
			}
		}
		if (!missingOptions.isEmpty()) {
			throw new IllegalArgumentException("Following required options are missing: " + missingOptions);
		}
		isParsed = true;
		return this;
	}

	private enum ParseMode {
		PARSE_OPTION, PARSE_VALUE;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String name) {
		try {
			return getByPlaceholderName(name);
		} catch (IllegalArgumentException ex) {
		}
		try {
			return getByLongName(name);
		} catch (IllegalArgumentException ex) {
		}
		return getByShortName(name);
	}

	public <T> T getOrDefault(String name, T defaultValue) {
		T value = get(name);
		if (null == value) {
			return defaultValue;
		}
		return value;
	}

	public <T> T getByLongName(String longName) {
		return getFromNameMap(longName, optionsByLongName);
	}

	public <T> T getByShortName(String shortName) {
		return getFromNameMap(shortName, optionsByShortName);
	}

	public <T> T getByPlaceholderName(String placeholderName) {
		return getFromNameMap(placeholderName, optionsByPlaceholderName);
	}

	@SuppressWarnings("unchecked")
	private <T> T getFromNameMap(String name, Map<String, Option<?>> nameMap) {
		if (!isParsed) {
			throw new IllegalStateException("Command line arguments need to be parsed before they can be retrieved.");
		}
		Option<?> option = nameMap.get(name);
		if (null != option) {
			T optionValue = (T) optionInstances.get(option);
			if (null != optionValue) {
				return optionValue;
			}
			return (T) option.getDefaultValue();
		}
		throw new IllegalArgumentException("Option '" + name + "' not found.");
	}

	private String getHelpAndUsage(boolean isGetHelp) {
		StringBuilder usageSb = new StringBuilder("Usage: ");
		StringBuilder helpSb = new StringBuilder();
		List<Option<?>> options = new ArrayList<>(optionInstances.keySet());
		for (Option<?> opt : options) {
			if (!opt.isRequired()) {
				usageSb.append("[");
			}
			boolean hasLongName = null != opt.getLongName() && !opt.getLongName().isEmpty();
			boolean hasShortName = null != opt.getShortName() && !opt.getShortName().isEmpty();
			boolean hasPlaceholderName = null != opt.getPlaceholderName() && !opt.getPlaceholderName().isEmpty();
			if (hasShortName) {
				usageSb.append(shortPrefix);
				usageSb.append(opt.getShortName());
				helpSb.append(shortPrefix);
				helpSb.append(opt.getShortName());
			}
			if (hasLongName && hasShortName) {
				usageSb.append("|");
				helpSb.append(", ");
			}
			if (hasLongName) {
				usageSb.append(longPrefix);
				usageSb.append(opt.getLongName());
				helpSb.append(longPrefix);
				helpSb.append(opt.getLongName());
			}
			if ((hasShortName || hasLongName) && hasPlaceholderName) {
				usageSb.append(" ");
				helpSb.append(" ");
			}
			if (hasPlaceholderName) {
				usageSb.append("<");
				usageSb.append(opt.getPlaceholderName());
				usageSb.append(">");
				helpSb.append("<");
				helpSb.append(opt.getPlaceholderName());
				helpSb.append(">");
			}
			if (!opt.isRequired()) {
				usageSb.append("]");
			}
			usageSb.append(" ");
			helpSb.append(System.lineSeparator());
			helpSb.append("\t");
			helpSb.append(opt.getDescription());
			helpSb.append(System.lineSeparator());
			if (opt.getType().isEnum()) {
				helpSb.append("\t");
				String possibleEnums = String.join(", ", Arrays.stream(opt.getType().getEnumConstants())
						.map(e -> ((Enum) e).name())
						.collect(Collectors.toList()));
				helpSb.append("Possible values: ");
				helpSb.append(possibleEnums);
				helpSb.append(System.lineSeparator());
			}
			if (null != opt.getDefaultValue()) {
				helpSb.append("\t");
				helpSb.append("Default: ");
				helpSb.append(opt.getDefaultValue().toString());
				helpSb.append(System.lineSeparator());
			}
			if (opt.isRequired()) {
				helpSb.append("\t");
				helpSb.append("(required)");
				helpSb.append(System.lineSeparator());
			}
		}
		if (isGetHelp) {
			if (null != description) {
				usageSb.insert(0, System.lineSeparator());
				usageSb.insert(0, System.lineSeparator());
				usageSb.insert(0, description);
			}
			usageSb.append(System.lineSeparator());
			usageSb.append(System.lineSeparator());
			usageSb.append(helpSb);
		}
		return usageSb.toString();
	}

	public String getUsage() {
		return getHelpAndUsage(false);
	}

	public String getHelp() {
		return getHelpAndUsage(true);
	}

	public void printUsage() {
		System.out.println(getUsage());
	}

	public void printHelp() {
		System.out.println(getHelp());
	}

	public static Builder create() {
		return new Builder();
	}

	public static class Builder {

		private final List<Option<?>> options = new ArrayList<>();
		private String longPrefix = "--";
		private String shortPrefix = "-";
		private String description;
		private boolean isCaseSensitiveLongOptions = true;
		private boolean isCaseSensitiveShortOptions = true;
		private boolean isCaseSensitiveEnumValues = false;
		private boolean isAllowAbbrevEnumValues = true;
		private boolean isPrintUsageOnError = true;
		private boolean isExitOnHelpOrError = true;
		private boolean isAddHelpOptions = true;

		public Builder() {
		}

		public CommandLineOptions build() {
			if (isAddHelpOptions) {
				help("help", "h", "Print detailed usage instructions for this program.");
			}
			return new CommandLineOptions(description, Collections.unmodifiableList(options), longPrefix, shortPrefix,
					isCaseSensitiveLongOptions, isCaseSensitiveShortOptions, isCaseSensitiveEnumValues, isAllowAbbrevEnumValues,
					isPrintUsageOnError, isExitOnHelpOrError);
		}

		public <T> Builder required(String longName, String shortName, String placeholderName, Class<T> valueType, String description) {
			options.add(new Option<>(true, longName, shortName, placeholderName, valueType, null, description, 1));
			return this;
		}

		public <T> Builder optional(String longName, String shortName, String placeholderName, Class<T> valueType, String description) {
			return optional(longName, shortName, placeholderName, valueType, null, description);
		}

		public <T> Builder optional(String longName, String shortName, String placeholderName, Class<T> valueType, T defaultValue, String description) {
			options.add(new Option<>(false, longName, shortName, placeholderName, valueType, defaultValue, description, 1));
			return this;
		}

		public Builder toggle(String longName, String shortName, String description) {
			options.add(new Option<>(false, longName, shortName, null, Boolean.class, null, description, 0));
			return this;
		}

		public Builder help(String longName, String shortName, String description) {
			options.add(new Option<>(false, longName, shortName, null, Boolean.class, null, description, 0, true));
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public <T> Builder required(String placeholderName, Class<T> valueType, String description) {
			options.add(new Option<>(true, null, null, placeholderName, valueType, null, description, 0));
			return this;
		}

		public <T> Builder optional(String placeholderName, Class<T> valueType, String description) {
			return optional(placeholderName, valueType, null, description);
		}

		public <T> Builder optional(String placeholderName, Class<T> valueType, T defaultValue, String description) {
			options.add(new Option<>(false, null, null, placeholderName, valueType, defaultValue, description, 0));
			return this;
		}

		public Builder withLongPrefix(String longPrefix) {
			this.longPrefix = longPrefix;
			return this;
		}

		public Builder withShortPrefix(String shortPrefix) {
			this.shortPrefix = shortPrefix;
			return this;
		}

		public Builder isCaseSensitiveLongOptions(boolean isCaseSensitive) {
			this.isCaseSensitiveLongOptions = isCaseSensitive;
			return this;
		}

		public Builder isCaseSensitiveShortOptions(boolean isCaseSensitive) {
			this.isCaseSensitiveShortOptions = isCaseSensitive;
			return this;
		}

		public Builder isCaseSensitiveEnumValues(boolean isCaseSensitive) {
			this.isCaseSensitiveEnumValues = isCaseSensitive;
			return this;
		}

		public Builder isAllowAbbrevEnumValues(boolean isAbbreviationAllowed) {
			this.isAllowAbbrevEnumValues = isAbbreviationAllowed;
			return this;
		}

		public Builder exitOnHelpOrError(boolean isExitOnHelpOrError) {
			this.isExitOnHelpOrError = isExitOnHelpOrError;
			return this;
		}

		public Builder printUsageOnError(boolean isPrintUsageOnError) {
			this.isPrintUsageOnError = isPrintUsageOnError;
			return this;
		}

		public Builder addHelpOptions(boolean isAddHelpOptions) {
			this.isAddHelpOptions = isAddHelpOptions;
			return this;
		}
	}

	private static class Option<T> {

		private final boolean isRequired;
		private final String longName;
		private final String shortName;
		private final String placeholderName;
		private final Class<T> type;
		private final String description;
		private final int arity;
		private final T defaultValue;
		private final boolean isHelp;

		Option(boolean isRequired, String longName, String shortName, String placeholderName, Class<T> type, T defaultValue, String description, int arity) {
			this(isRequired, longName, shortName, placeholderName, type, defaultValue, description, arity, false);
		}

		Option(boolean isRequired, String longName, String shortName, String placeholderName, Class<T> type, T defaultValue, String description, int arity, boolean isHelp) {
			this.isRequired = isRequired;
			this.longName = longName;
			this.shortName = shortName;
			this.placeholderName = placeholderName;
			this.type = type;
			this.description = description;
			this.defaultValue = defaultValue;
			this.arity = arity;
			this.isHelp = isHelp;
		}

		public boolean isRequired() {
			return isRequired;
		}

		public String getLongName() {
			return longName;
		}

		public String getShortName() {
			return shortName;
		}

		public String getPlaceholderName() {
			return placeholderName;
		}

		public Class<T> getType() {
			return type;
		}

		public String getDescription() {
			return description;
		}

		public T getDefaultValue() {
			return defaultValue;
		}

		public int getArity() {
			return arity;
		}

		public boolean isHelp() {
			return isHelp;
		}

		@SuppressWarnings("unchecked")
		public T map(String value, boolean isCaseSensitiveEnumValues, boolean isAllowAbbrevEnumValues) {
			if (type.equals(String.class)) {
				return (T) value;
			} else if (type.equals(Boolean.class)) {
				return (T) Boolean.valueOf(value);
			} else if (type.equals(Integer.class)) {
				return (T) Integer.valueOf(value);
			} else if (type.equals(Long.class)) {
				return (T) Long.valueOf(value);
			} else if (type.equals(Double.class)) {
				return (T) Double.valueOf(value);
			} else if (type.isEnum()) {
				return (T) createEnum((Class<? extends Enum>) type, value, isCaseSensitiveEnumValues, isAllowAbbrevEnumValues);
			}
			throw new IllegalArgumentException("No mapping defined for class '" + type + "'.");
		}

		@SuppressWarnings("unchecked")
		private <T extends Enum<T>> T createEnum(Class<T> type, String value, boolean isCaseSensitiveEnumValues, boolean isAllowAbbrevEnumValues) {
			if (!isCaseSensitiveEnumValues && !isAllowAbbrevEnumValues) {
				return Enum.valueOf(type, value);
			}
			String v;
			if (isCaseSensitiveEnumValues) {
				v = value;
			} else {
				v = value.toUpperCase();
			}
			T[] enumConstants = type.getEnumConstants();
			Object[] possibleValues = Arrays.stream(enumConstants)
					.map(e -> isCaseSensitiveEnumValues ? new Object[]{e, e.name()} : new Object[]{e, e.name().toUpperCase()})
					.filter(tuple -> isAllowAbbrevEnumValues ? ((String) tuple[1]).startsWith(v) : ((String) tuple[1]).equals(v))
					.map(tuple -> (T) tuple[0])
					.toArray();
			if (possibleValues.length == 0) {
				throw new IllegalArgumentException("No matching enumeration constant for value '" + value + "' found.");
			}
			if (possibleValues.length > 1) {
				throw new IllegalArgumentException("Ambiguous enumeration constant for value '" + value + "'");
			}
			return (T) possibleValues[0];
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 53 * hash + Objects.hashCode(this.longName);
			hash = 53 * hash + Objects.hashCode(this.shortName);
			hash = 53 * hash + Objects.hashCode(this.placeholderName);
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Option<?> other = (Option<?>) obj;
			if (!Objects.equals(this.longName, other.longName)) {
				return false;
			}
			if (!Objects.equals(this.shortName, other.shortName)) {
				return false;
			}
			if (!Objects.equals(this.placeholderName, other.placeholderName)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			if (null != longName) {
				return longName;
			}
			if (null != shortName) {
				return shortName;
			}
			return placeholderName;
		}

	}

}
