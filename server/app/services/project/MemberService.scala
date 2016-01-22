/**
 * Copyright(c) 2016 Knut Petter Meen, all rights reserved.
 */
package services.project

import com.google.inject.{Inject, Singleton}
import core.lib.SuccessOrFailure
import models.base.Id
import models.party.PartyBaseTypes.{OrganisationId, UserId}
import models.project.{Member, MemberId, ProjectId}
import repository.MemberRepository

@Singleton
class MemberService @Inject() (val memberRepository: MemberRepository) {

  def save(m: Member): SuccessOrFailure = memberRepository.save(m)

  def findById(mid: MemberId): Option[Member] = memberRepository.findById(mid)

  def listBy[A <: Id](id: A): Seq[Member] = memberRepository.listBy(id)

  def listByUserId(uid: UserId): Seq[Member] = memberRepository.listByUserId(uid)

  def listByProjectId(pid: ProjectId): Seq[Member] = memberRepository.listByProjectId(pid)

  def listByOrganisationId(oid: OrganisationId): Seq[Member] = memberRepository.listByOrganisationId(oid)

}
