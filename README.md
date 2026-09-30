# cpt-lab1-krushelnytskyi (working title)

Java 21 console application for the "Electronics store" domain. The program reads a
semicolon-separated UTF-8 CSV file of product records (`name;category;price;warrantyMonths;stock`),
validates every line, skips invalid ones with a diagnostic message, and prints a report with the
computed metrics: number of valid records, average price, longest warranty and total stock.
The repository currently contains only the project infrastructure; the implementation follows.
