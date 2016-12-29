/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package core.security.filters

import com.google.inject.Inject
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSFilter
import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter

/**
 * Provides filters.
 */
class Filters @Inject() (
    corsFilter: CORSFilter,
    csrfFilter: CSRFFilter,
    securityHeadersFilter: SecurityHeadersFilter
) extends HttpFilters {

  override def filters: Seq[EssentialFilter] = Seq(
    corsFilter,
    //    csrfFilter,
    securityHeadersFilter
  )

}