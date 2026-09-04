package me.ilker.balance_tracker

import io.ktor.resources.Resource

@Resource("/register")
class Registration

@Resource("/login")
class Login

@Resource("/link/token")
class LinkTokenRoute

@Resource("/link")
class LinkRoute

@Resource("/logout")
class Logout