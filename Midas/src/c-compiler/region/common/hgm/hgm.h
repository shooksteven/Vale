#ifndef REGION_COMMON_HGM_HGM_H_
#define REGION_COMMON_HGM_HGM_H_

#include <llvm-c/Core.h>
#include <function/expressions/shared/afl.h>
#include "globalstate.h"
#include "function/function.h"
#include <region/common/fatweaks/fatweaks.h>

class HybridGenerationalMemory {
public:
  HybridGenerationalMemory(
      GlobalState* globalState_,
      ControlBlock* controlBlock_,
      IReferendStructsSource* referendStructsSource_,
      IWeakRefStructsSource* weakRefStructsSource_,
      bool elideChecksForKnownLive_,
      bool limitMode_,
      StructReferend* anyMT);

  void mainSetup(FunctionState* functionState, LLVMBuilderRef builder);
  void mainCleanup(FunctionState* functionState, LLVMBuilderRef builder);

  Ref assembleWeakRef(
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Reference* sourceType,
      Reference* targetType,
      Ref sourceRef);

  WeakFatPtrLE weakStructPtrToGenWeakInterfacePtr(
      GlobalState *globalState,
      FunctionState *functionState,
      LLVMBuilderRef builder,
      WeakFatPtrLE sourceRefLE,
      StructReferend *sourceStructReferendM,
      Reference *sourceStructTypeM,
      InterfaceReferend *targetInterfaceReferendM,
      Reference *targetInterfaceTypeM);

  // Makes a non-weak interface ref into a weak interface ref
  WeakFatPtrLE assembleInterfaceWeakRef(
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Reference* sourceType,
      Reference* targetType,
      InterfaceReferend* interfaceReferendM,
      InterfaceFatPtrLE sourceInterfaceFatPtrLE);

  WeakFatPtrLE assembleStructWeakRef(
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Reference* structTypeM,
      Reference* targetTypeM,
      StructReferend* structReferendM,
      WrapperPtrLE objPtrLE);

  WeakFatPtrLE assembleKnownSizeArrayWeakRef(
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Reference* sourceKSAMT,
      KnownSizeArrayT* knownSizeArrayMT,
      Reference* targetKSAWeakRefMT,
      WrapperPtrLE objPtrLE);

  WeakFatPtrLE assembleUnknownSizeArrayWeakRef(
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Reference* sourceType,
      UnknownSizeArrayT* unknownSizeArrayMT,
      Reference* targetUSAWeakRefMT,
      WrapperPtrLE sourceRefLE);

  LLVMValueRef lockGenFatPtr(
      AreaAndFileAndLine from,
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Reference* refM,
      WeakFatPtrLE weakRefLE,
      bool weakRefKnownLive);

  void innerNoteWeakableDestroyed(
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Reference* concreteRefM,
      ControlBlockPtrLE controlBlockPtrLE);


  void aliasWeakRef(
      AreaAndFileAndLine from,
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Reference* weakRefMT,
      Ref weakRef);

  void discardWeakRef(
      AreaAndFileAndLine from,
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Reference* weakRefMT,
      Ref weakRef);

  LLVMValueRef getIsAliveFromWeakFatPtr(
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Reference* weakRefM,
      WeakFatPtrLE weakFatPtrLE,
      bool knownLive);

  Ref getIsAliveFromWeakRef(
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Reference* weakRefM,
      Ref weakRef,
      bool knownLive);

  LLVMValueRef fillWeakableControlBlock(
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Referend* referendM,
      LLVMValueRef controlBlockLE);

  WeakFatPtrLE weakInterfaceRefToWeakStructRef(
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Reference* weakInterfaceRefMT,
      WeakFatPtrLE weakInterfaceFatPtrLE);

  void buildCheckWeakRef(
      AreaAndFileAndLine checkerAFL,
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Reference* weakRefM,
      Ref weakRef);

  static LLVMTypeRef makeWeakRefHeaderStruct(GlobalState* globalState, RegionId* regionId);

  WrapperPtrLE getHalfProtectedPtr(
      FunctionState* functionState, LLVMBuilderRef builder, Reference* reference, LLVMTypeRef wrapperStructPtrLT);

  void addToUndeadCycle(
      FunctionState* functionState,
      LLVMBuilderRef builder,
      Reference* refMT,
      WrapperPtrLE uncastedObjWrapperPtrLE);

private:
  LLVMValueRef getTargetGenFromWeakRef(
      LLVMBuilderRef builder,
      IWeakRefStructsSource* weakRefStructsSource,
      Referend* referend,
      WeakFatPtrLE weakRefLE);

  Prototype* makeCleanupLoopFunction();
  Prototype* makeCleanupIterFunction();

  Prototype* makeMainSetupFunction();

  Prototype* makeMainCleanupFunction();

  Prototype* cleanupIterPrototype = nullptr;

  GlobalState* globalState = nullptr;
  ControlBlock* controlBlock = nullptr;
  FatWeaks fatWeaks;
  IReferendStructsSource* referendStructsSource;
  IWeakRefStructsSource* weakRefStructsSource;

  LLVMBuilderRef setupBuilder = nullptr;

  LLVMTypeRef undeadCycleNodeLT = nullptr;
  LLVMValueRef undeadCycleHeadNodePtrPtrLE = nullptr;

  bool elideChecksForKnownLive = false;

  // If true, then pretend all references are known live, dont fill in any generations, basically
  // pretend to be unsafe mode as much as possible.
  // This is to see the theoretical maximum speed of HGM, and where its slowdowns are.
  bool limitMode = false;

  // Points to an object whose control block is in an unprotected page, and the contents of the object
  // is in a protected page.
  // (Well, actually, the object itself is an empty struct, so i guess it's all in the protected page.)
  // This is so we have an object whose tethered bit we can modify but no other part of it.
  LLVMValueRef halfProtectedI8PtrPtrLE = nullptr;

  StructReferend* anyMT = nullptr;

  std::unordered_map<Referend*, LLVMValueRef, AddressHasher<Referend*>> globalNullPtrPtrByReferend;
};

#endif