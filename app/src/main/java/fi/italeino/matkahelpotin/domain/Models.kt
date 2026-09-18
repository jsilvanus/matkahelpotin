fun selectMileagePolicy(policies: List<MileagePolicy>, date: LocalDate, jurisdiction: String = "FI", scope: MileagePolicyScope = MileagePolicyScope.BUSINESS_TRIP): MileagePolicy? =
    policies
        .filter {
            it.jurisdiction == jurisdiction &&
                it.scope == scope &&
                !date.isBefore(it.validFrom) &&
                (it.validUntil == null || date.isBefore(it.validUntil))
        }
        .maxWithOrNull(compareBy<MileagePolicy> { it.validFrom }.thenBy { it.id.toString() })

fun selectAnnualMileagePolicy(
    policies: List<MileagePolicy>,
    year: Int,
    asOf: LocalDate,
    jurisdiction: String = "FI",
    scope: MileagePolicyScope = MileagePolicyScope.BUSINESS_TRIP,
): MileagePolicy? {
    require(asOf.year == year || asOf.year == year + 1) { "asOf must be within the policy year or its following year" }
    return selectMileagePolicy(
        policies,
        asOf.coerceAtMost(LocalDate.of(year, 12, 31)),
        jurisdiction,
        scope,
    )?.takeIf { it.year == year }
}