package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._
import mapper._

import code.model._


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("code")

    val preMenu = List(Menu.i("Home") / "index")
    val scaffoldMenu = ScaffoldList.menu
    val postMenu = List(Menu.i("Static") / "static" / **)

    // Build SiteMap
    LiftRules.setSiteMap(SiteMap.build((preMenu ++ scaffoldMenu ++ postMenu).toArray))

    // Use jQuery 1.4
    LiftRules.jsArtifacts = net.liftweb.http.js.jquery.JQuery14Artifacts

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Fadeout for Notices, not Errors
    LiftRules.noticesAutoFadeOut.default.set( (notices: NoticeType.Value) => {
      notices match {
        case NoticeType.Notice => Full((2 seconds, 2 seconds))
        case _ => Empty
      }}) 
      
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))    

    // Handle the Database
    if (!DB.jndiJdbcConnAvailable_? && Props.require("db.driver", "db.url").isEmpty) {
      val vendor = new StandardDBVendor(
           Props.get("db.driver") openTheBox,
           Props.get("db.url") openTheBox,
           Props.get("db.user"), Props.get("db.password"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)
      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
      ScaffoldList.modelList foreach { t => Schemifier.schemify(true, Schemifier.infoF _, t) }
      S.addAround(DB.buildLoanWrapper)
    }
  }
}
