/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.authentication

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import net.scalytica.symbiotic.css.LoginStyle
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.Name
import net.scalytica.symbiotic.models.party.{CreateUser, CreateUserValidity}
import net.scalytica.symbiotic.routing.SymbioticRouter
import net.scalytica.symbiotic.routing.SymbioticRouter.View

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.ScalaCssReact._

object RegistrationForm {

  case class Props(ctl: RouterCtl[View], createUsr: CreateUser)

  case class State(
    ctl: RouterCtl[View],
    createUsr: CreateUser,
    validity: CreateUserValidity = CreateUserValidity(),
    error: Boolean = false
  )

  class Backend($: BackendScope[Props, State]) {

    private def isValid(e: ReactEventI) = e.target.checkValidity()

    def onKeyEnter(e: ReactKeyboardEventI) =
      if (e.key == "Enter")
        // TODO: validate form fields.
        $.state.flatMap(s => doRegister(s.createUsr))
      else Callback.empty

    def onFirstNameChange(e: ReactEventI) = {
      e.persist()
      $.modState { s =>
        val maybeName = Option(e.target.value)
        val n = s.createUsr.name.map(_.copy(first = maybeName)).orElse(Some(Name(first = maybeName)))
        s.copy(createUsr = s.createUsr.copy(name = n))
      }
    }

    def onMiddleNameChange(e: ReactEventI) = {
      e.persist()
      $.modState { s =>
        val maybeName = Option(e.target.value)
        val n = s.createUsr.name.map(_.copy(middle = maybeName)).orElse(Some(Name(middle = maybeName)))
        s.copy(createUsr = s.createUsr.copy(name = n))
      }
    }

    def onLastNameChange(e: ReactEventI) = {
      e.persist()
      $.modState { s =>
        val maybeName = Option(e.target.value)
        val n = s.createUsr.name.map(_.copy(last = maybeName)).orElse(Some(Name(last = maybeName)))
        s.copy(createUsr = s.createUsr.copy(name = n))
      }
    }

    def onDateOfBirthChange(e: ReactEventI) = {
      e.persist()
      $.modState { s =>
        val dob = Option(e.target.value)
        s.copy(
          createUsr = s.createUsr.copy(dateOfBirth = dob),
          validity = s.validity.copy(dateOfBirthValid = dob.map(_ => isValid(e)))
        )
      }
    }

    def onUsernameChange(e: ReactEventI): Callback = {
      e.persist()
      $.state.map { currState =>
        val maybeLongEnough = Option(e.target.value).filter(_.length >= 3)
        val nextCreateUser = currState.createUsr.copy(username = e.target.value)
        maybeLongEnough.map { _ =>
          Callback.future {
            CreateUserValidity.validateUsername(e.target.value).map(valid =>
              $.modState(s =>
                s.copy(
                  createUsr = nextCreateUser,
                  validity = s.validity.copy(usernameValid = valid)
                )
              )
            )
          }
        }.getOrElse {
          $.modState(s => s.copy(
            createUsr = nextCreateUser,
            validity = s.validity.copy(usernameValid = false)
          ))
        }.runNow()
      }
    }

    def onEmailChange(e: ReactEventI) = {
      e.persist()
      $.modState { s =>
        s.copy(
          createUsr = s.createUsr.copy(email = e.target.value),
          validity = s.validity.copy(emailValid = isValid(e))
        )
      }
    }

    def onPassChange(e: ReactEventI) = {
      e.persist()
      $.modState { s =>
        s.copy(
          createUsr = s.createUsr.copy(password1 = e.target.value),
          validity = s.validity.copy(password1Valid = e.target.value.length >= 6)
        )
      }
    }

    def onVerifyPassChange(e: ReactEventI) = {
      e.persist()
      $.modState { s =>
        s.copy(
          createUsr = s.createUsr.copy(password2 = e.target.value),
          validity = s.validity.copy(password2Valid = s.createUsr.password1 == e.target.value)
        )
      }
    }

    def doRegister(cusr: CreateUser): Callback =
      $.state.map { s =>
        Callback.future(
          CreateUser.register(cusr).map { success =>
            if (success) {
              log.debug(s"Registered user ${cusr.username}")
              s.ctl.set(SymbioticRouter.Home)
            }
            else {
              log.error("Unable to register user")
              log.debug(s.createUsr)
              $.modState(_.copy(error = true))
            }
          }
        ).runNow()
      }

    def render(props: Props, state: State) = {
      <.div(LoginStyle.loginCard, ^.onKeyPress ==> onKeyEnter,
        if (state.error) {
          <.div(^.className := "alert alert-danger", ^.role := "alert",
            "An error occured trying to register your data. Please try again later."
          )
        } else {
          ""
        },
        <.form(
          <.div(^.className := "form-group",
            <.label(^.`for` := "firstName", "First name"),
            <.input(
              ^.id := "firstName",
              ^.className := "form-control",
              ^.tpe := "text",
              ^.value := state.createUsr.name.flatMap(_.first).getOrElse(""),
              ^.onChange ==> onFirstNameChange
            )
          ),
          <.div(^.className := "form-group",
            <.label(^.`for` := "middleName", "Middle name"),
            <.input(
              ^.id := "middleName",
              ^.className := "form-control",
              ^.tpe := "text",
              ^.value := state.createUsr.name.flatMap(_.middle).getOrElse(""),
              ^.onChange ==> onMiddleNameChange
            )
          ),
          <.div(^.className := "form-group",
            <.label(^.`for` := "lastName", "Last name"),
            <.input(
              ^.id := "lastName",
              ^.className := "form-control",
              ^.tpe := "text",
              ^.value := state.createUsr.name.flatMap(_.last).getOrElse(""),
              ^.onChange ==> onLastNameChange
            )
          ),
          <.div(LoginStyle.formElemHasError(state.validity.usernameValid),
            <.label(^.`for` := "registerUsername", "Username"),
            <.input(
              ^.id := "registerUsername",
              ^.className := "form-control",
              ^.tpe := "text",
              ^.value := state.createUsr.username,
              ^.onChange ==> onUsernameChange
            )
          ),
          <.div(LoginStyle.formElemHasError(state.validity.emailValid),
            <.label(^.`for` := "emailAddress", "Email address"),
            <.input(
              ^.id := "emailAddress",
              ^.className := "form-control",
              ^.tpe := "email",
              ^.value := state.createUsr.email,
              ^.onChange ==> onEmailChange
            )
          ),
          <.div(LoginStyle.formElemHasError(state.validity.password1Valid),
            <.label(^.`for` := "registerPassword", "Password"),
            <.input(
              ^.id := "registerPassword",
              ^.className := "form-control",
              ^.tpe := "password",
              ^.value := state.createUsr.password1,
              ^.onChange ==> onPassChange
            )
          ),
          <.div(LoginStyle.formElemHasError(state.validity.password2Valid),
            <.label(^.`for` := "verifyPassword", "Verify Password"),
            <.input(
              ^.id := "verifyPassword",
              ^.className := "form-control",
              ^.tpe := "password",
              ^.value := state.createUsr.password2,
              ^.onChange ==> onVerifyPassChange
            )
            //          ),
            //          <.div(^.className := s.validity.dateOfBirthValid.filter(b => !b).map(_ => "from-group has-error").getOrElse("form-group"),
            //            <.label(^.`for` := "dateOfBirth", "Date of birth"),
            //            <.input(
            //              ^.id := "dateOfBirth",
            //              ^.className := "form-control",
            //              ^.tpe := "date",
            //              ^.defaultValue := "YYYY-MM-DD",
            //              ^.value := s.createUsr.dateOfBirth.getOrElse(""),
            //              ^.onChange ==> onDateOfBirthChange
            //            )
          )
        ),
        <.div(^.className := "card-action no-border text-right",
          <.input(
            ^.className := "btn btn-success",
            ^.tpe := "button",
            ^.value := "Register",
            ^.onClick --> doRegister(state.createUsr)
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("RegistrationForm")
    .initialState_P(p => State(p.ctl, p.createUsr))
    .renderBackend[Backend]
    .build


  def apply(p: Props) = component(p)

  def apply(ctl: RouterCtl[View]) = component(Props(ctl, CreateUser.empty))
}
