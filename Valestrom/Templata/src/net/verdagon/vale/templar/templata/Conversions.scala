package net.verdagon.vale.templar.templata

import net.verdagon.vale.astronomer._
import net.verdagon.vale.parser._
import net.verdagon.vale.scout.CodeLocationS
import net.verdagon.vale.scout.rules._
import net.verdagon.vale.{scout => s}
import net.verdagon.vale.templar.{types => t}
import net.verdagon.vale.templar.types._
import net.verdagon.vale.vimpl

object Conversions {
  def evaluateMutability(mutability: MutabilityP): Mutability = {
    mutability match {
      case MutableP => Mutable
      case ImmutableP => Immutable
    }
  }

  def evaluatePermission(permission: PermissionP): Permission = {
    permission match {
      case ReadonlyP => Readonly
      case ReadwriteP => Readwrite
//      case ExclusiveReadwriteP => ExclusiveReadwrite
      case _ => vimpl()
    }
  }

  def evaluateLocation(location: LocationP): Location = {
    location match {
      case InlineP => Inline
      case YonderP => Yonder
    }
  }

  def evaluateVariability(variability: VariabilityP): Variability = {
    variability match {
      case FinalP => Final
      case VaryingP => Varying
    }
  }

  def evaluateOwnership(ownership: OwnershipP): Ownership = {
    ownership match {
      case OwnP => Own
      case ConstraintP => Constraint
      case WeakP => Weak
      case ShareP => Share
    }
  }

  def evaluateMaybeOwnership(maybeOwnership: Option[OwnershipP]): Option[Ownership] = {
    maybeOwnership.map({
      case OwnP => Own
      case ConstraintP => Constraint
      case WeakP => Weak
      case ShareP => Share
    })
  }

  def evaluateRefCountCategory(refCountCategory: s.RefCountCategory): t.RefCountCategory = {
    refCountCategory match {
      case s.MemberRefCount => t.MemberRefCount
      case s.VariableRefCount => t.VariableRefCount
      case s.RegisterRefCount => t.RegisterRefCount
    }
  }

  def unevaluateOwnership(ownership: Ownership): OwnershipP = {
    ownership match {
      case Own => OwnP
      case Constraint => ConstraintP
      case Weak => WeakP
      case Share => ShareP
    }
  }

  def unevaluatePermission(permission: Permission): PermissionP = {
    permission match {
      case Readonly => ReadonlyP
      case Readwrite => ReadwriteP
//      case ExclusiveReadwrite => ExclusiveReadwriteP
    }
  }

  def unevaluateMutability(mutability: Mutability): MutabilityP = {
    mutability match {
      case Mutable => MutableP
      case Immutable => ImmutableP
    }
  }

  def unevaluateTemplataType(tyype: ITemplataType): ITypeSR = {
    tyype match {
      case CoordTemplataType => CoordTypeSR
      case KindTemplataType => KindTypeSR
      case IntegerTemplataType => IntTypeSR
      case BooleanTemplataType => BoolTypeSR
      case PrototypeTemplataType => PrototypeTypeSR
      case MutabilityTemplataType => MutabilityTypeSR
      case PermissionTemplataType => PermissionTypeSR
      case LocationTemplataType => LocationTypeSR
      case OwnershipTemplataType => OwnershipTypeSR
      case VariabilityTemplataType => VariabilityTypeSR
      case TemplateTemplataType(_, _) => vimpl() // can we even specify template types in the syntax?
    }
  }
}
