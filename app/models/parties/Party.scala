/**
 * Copyright(c) 2014 Knut Petter Meen, all rights reserved.
 */
package models.parties

sealed trait Party

trait Organization extends Party

trait Individual extends Party
