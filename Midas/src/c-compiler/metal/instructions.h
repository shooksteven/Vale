
#ifndef VALE_INSTRUCTIONS_H_
#define VALE_INSTRUCTIONS_H_

class Expression;
class IRegister;
class ReferenceRegister;
class AddressRegister;
class Local;
class VariableId;
class StackHeight;

enum class RefCountCategory {
    VARIABLE_REF_COUNT,
    MEMBER_REF_COUNT,
    REGISTER_REF_COUNT
};

class Expression {
public:
    virtual ~Expression() {}

//    virtual Reference* getResultType() const = 0;
};

class ConstantI64 : public Expression {
public:
  int value;

  ConstantI64(
      int value_)
      : value(value_) {}
};

class ConstantBool : public Expression {
public:
  bool value;

  ConstantBool(
      bool value_)
      : value(value_) {}
};


class ConstantVoid : public Expression {
public:
};


class ConstantStr : public Expression {
public:
  std::string value;

  ConstantStr(
      const std::string &value_) :
      value(value_) {}
};


class ConstantF64 : public Expression {
public:
  double value;

  ConstantF64(
      const double &value_) :
      value(value_) {}
};


class Argument : public Expression {
public:
  Reference* resultType;
  int argumentIndex;
  Argument(
      Reference* resultType_,
      int argumentIndex_) :
    resultType(resultType_),
    argumentIndex(argumentIndex_) {}
};


class Stackify : public Expression {
public:
  Expression* sourceExpr;
  Local* local;
  bool knownLive;
  std::string maybeName;

  Stackify(
      Expression* sourceExpr_,
      Local* local_,
      bool knownLive_,
      std::string maybeName_) :
    sourceExpr(sourceExpr_),
    local(local_),
    knownLive(knownLive_),
    maybeName(maybeName_) {}
};


class Unstackify : public Expression {
public:
  Local* local;

  Unstackify(Local* local_) :
    local(local_) {}
};


class Destroy : public Expression {
public:
  Expression* structExpr;
  Reference* structType;
  std::vector<Reference*> localTypes;
  std::vector<Local*> localIndices;
  std::vector<bool> localsKnownLives;

  Destroy(
      Expression* structExpr_,
      Reference* structType_,
      std::vector<Reference*> localTypes_,
      std::vector<Local*> localIndices_,
      std::vector<bool> localsKnownLives_) :
      structExpr(structExpr_),
      structType(structType_),
      localTypes(localTypes_),
      localIndices(localIndices_),
      localsKnownLives(localsKnownLives_) {}
};


class StructToInterfaceUpcast : public Expression {
public:
  Expression* sourceExpr;
  Reference* sourceStructType;
  StructReferend* sourceStructReferend;
  Reference* targetInterfaceType;
  InterfaceReferend* targetInterfaceReferend;

  StructToInterfaceUpcast(
      Expression* sourceExpr_,
      Reference* sourceStructType_,
      StructReferend* sourceStructReferend_,
      Reference* targetInterfaceType_,
      InterfaceReferend* targetInterfaceReferend_) :
      sourceExpr(sourceExpr_),
      sourceStructType(sourceStructType_),
      sourceStructReferend(sourceStructReferend_),
      targetInterfaceType(targetInterfaceType_),
      targetInterfaceReferend(targetInterfaceReferend_) {}
};

class InterfaceToInterfaceUpcast : public Expression {
public:
  Expression* sourceExpr;
  InterfaceReferend* targetInterfaceRef;
};

class LocalStore : public Expression {
public:
  Local* local;
  Expression* sourceExpr;
  std::string localName;
  bool knownLive;

  LocalStore(
      Local* local_,
      Expression* sourceExpr_,
      std::string localName_,
      bool knownLive_) :
      local(local_),
      sourceExpr(sourceExpr_),
      localName(localName_),
      knownLive(knownLive_) {}
};


class LocalLoad : public Expression {
public:
  Local* local;
  Ownership targetOwnership;
  std::string localName;

  LocalLoad(
      Local* local,
      Ownership targetOwnership,
      std::string localName) :
      local(local),
    targetOwnership(targetOwnership),
        localName(localName) {}
};


class WeakAlias : public Expression {
public:
  Expression* sourceExpr;
  Reference* sourceType;
  Referend* sourceReferend;
  Reference* resultType;

  WeakAlias(
      Expression* sourceExpr_,
      Reference* sourceType_,
      Referend* sourceReferend_,
      Reference* resultType_) :
    sourceExpr(sourceExpr_),
    sourceType(sourceType_),
    sourceReferend(sourceReferend_),
    resultType(resultType_) {}
};


class MemberStore : public Expression {
public:
  Expression* structExpr;
  Reference* structType;
  bool structKnownLive;
  int memberIndex;
  Expression* sourceExpr;
  Reference* resultType;
  std::string memberName;

  MemberStore(
      Expression* structExpr_,
      Reference* structType_,
      bool structKnownLive_,
      int memberIndex_,
      Expression* sourceExpr_,
      Reference* resultType_,
      std::string memberName_) :
    structExpr(structExpr_),
    structType(structType_),
    structKnownLive(structKnownLive_),
    memberIndex(memberIndex_),
    sourceExpr(sourceExpr_),
    resultType(resultType_),
    memberName(memberName_) {}
};

class NarrowPermission : public Expression {
public:
  Expression* sourceExpr;

  NarrowPermission(
      Expression* sourceExpr_) :
      sourceExpr(sourceExpr_) {}
};


class MemberLoad : public Expression {
public:
  Expression* structExpr;
  StructReferend* structId;
  Reference* structType;
  bool structKnownLive;
  int memberIndex;
  Ownership targetOwnership;
  Reference* expectedMemberType;
  Reference* expectedResultType;
  std::string memberName;

  MemberLoad(
      Expression* structExpr_,
      StructReferend* structId_,
      Reference* structType_,
      bool structKnownLive_,
      int memberIndex_,
      Ownership targetOwnership_,
      Reference* expectedMemberType_,
      Reference* expectedResultType_,
      std::string memberName_) :
    structExpr(structExpr_),
    structId(structId_),
    structType(structType_),
    structKnownLive(structKnownLive_),
    memberIndex(memberIndex_),
    targetOwnership(targetOwnership_),
    expectedMemberType(expectedMemberType_),
    expectedResultType(expectedResultType_),
    memberName(memberName_) {}
};


class NewArrayFromValues : public Expression {
public:
  std::vector<Expression*> sourceExprs;
  Reference* arrayRefType;
  KnownSizeArrayT* arrayReferend;

  NewArrayFromValues(
      std::vector<Expression*> sourceExprs_,
      Reference* arrayRefType_,
      KnownSizeArrayT* arrayReferend_) :
      sourceExprs(sourceExprs_),
      arrayRefType(arrayRefType_),
      arrayReferend(arrayReferend_) {}
};


class KnownSizeArrayStore : public Expression {
public:
  Expression* arrayExpr;
  Expression* indexExpr;
  Expression* sourceExpr;
};


class UnknownSizeArrayStore : public Expression {
public:
  Expression* arrayExpr;
  Reference* arrayType;
  UnknownSizeArrayT* arrayReferend;
  bool arrayKnownLive;
  Expression* indexExpr;
  Reference* indexType;
  Referend* indexReferend;
  Expression* sourceExpr;
  Reference* sourceType;
  Referend* sourceReferend;

  UnknownSizeArrayStore(
      Expression* arrayExpr_,
      Reference* arrayType_,
      UnknownSizeArrayT* arrayReferend_,
      bool arrayKnownLive_,
      Expression* indexExpr_,
      Reference* indexType_,
      Referend* indexReferend_,
      Expression* sourceExpr_,
      Reference* sourceType_,
      Referend* sourceReferend_) :
    arrayExpr(arrayExpr_),
    arrayType(arrayType_),
    arrayReferend(arrayReferend_),
    arrayKnownLive(arrayKnownLive_),
    indexExpr(indexExpr_),
    indexType(indexType_),
    indexReferend(indexReferend_),
    sourceExpr(sourceExpr_),
    sourceType(sourceType_),
    sourceReferend(sourceReferend_) {}
};


class UnknownSizeArrayLoad : public Expression {
public:
  Expression* arrayExpr;
  Reference* arrayType;
  UnknownSizeArrayT* arrayReferend;
  bool arrayKnownLive;
  Expression* indexExpr;
  Reference* indexType;
  Referend* indexReferend;
  Reference* resultType;
  Ownership targetOwnership;
  Reference* arrayElementType;

  UnknownSizeArrayLoad(
      Expression* arrayExpr_,
      Reference* arrayType_,
      UnknownSizeArrayT* arrayReferend_,
      bool arrayKnownLive_,
      Expression* indexExpr_,
      Reference* indexType_,
      Referend* indexReferend_,
      Reference* resultType_,
      Ownership targetOwnership_,
      Reference* arrayElementType_) :
    arrayExpr(arrayExpr_),
    arrayType(arrayType_),
    arrayReferend(arrayReferend_),
    arrayKnownLive(arrayKnownLive_),
    indexExpr(indexExpr_),
    indexType(indexType_),
    indexReferend(indexReferend_),
    resultType(resultType_),
    targetOwnership(targetOwnership_),
    arrayElementType(arrayElementType_) {}
};


class KnownSizeArrayLoad : public Expression {
public:
  Expression* arrayExpr;
  Reference* arrayType;
  KnownSizeArrayT* arrayReferend;
  bool arrayKnownLive;
  Expression* indexExpr;
  Reference* resultType;
  Ownership targetOwnership;
  Reference* arrayElementType;
  int arraySize;

  KnownSizeArrayLoad(
      Expression* arrayExpr_,
      Reference* arrayType_,
      KnownSizeArrayT* arrayReferend_,
      bool arrayKnownLive_,
      Expression* indexExpr_,
      Reference* resultType_,
      Ownership targetOwnership_,
      Reference* arrayElementType_,
      int arraySize_) :
    arrayExpr(arrayExpr_),
    arrayType(arrayType_),
    arrayReferend(arrayReferend_),
    arrayKnownLive(arrayKnownLive_),
    indexExpr(indexExpr_),
    resultType(resultType_),
    targetOwnership(targetOwnership_),
    arrayElementType(arrayElementType_),
    arraySize(arraySize_) {}
};


class Call : public Expression {
public:
  Prototype *function;
  std::vector<Expression *> argExprs;

  Call(
      Prototype *function_,
      std::vector<Expression *> argExprs_)
      : function(function_),
        argExprs(argExprs_) {}
};

class ExternCall : public Expression {
public:
    Prototype *function;
    std::vector<Expression *> argExprs;
    std::vector<Reference *> argTypes;

    ExternCall(
        Prototype *function_,
        std::vector<Expression *> argExprs_,
        std::vector<Reference *> argTypes_)
        : function(function_),
        argExprs(argExprs_),
        argTypes(argTypes_) {}
};


class InterfaceCall : public Expression {
public:
  std::vector<Expression*> argExprs;
  int virtualParamIndex;
  InterfaceReferend* interfaceRef;
  int indexInEdge;
  Prototype* functionType;

  InterfaceCall(
      std::vector<Expression*> argExprs_,
      int virtualParamIndex_,
      InterfaceReferend* interfaceRef_,
      int indexInEdge_,
      Prototype* functionType_) :
    argExprs(argExprs_),
    virtualParamIndex(virtualParamIndex_),
    interfaceRef(interfaceRef_),
    indexInEdge(indexInEdge_),
    functionType(functionType_) {}
};


class If : public Expression {
public:
  Expression* conditionExpr;
  Expression* thenExpr;
  Reference* thenResultType;
  Expression* elseExpr;
  Reference* elseResultType;
  Reference* commonSupertype;

  If(
      Expression * conditionExpr_,
      Expression * thenExpr_,
      Reference* thenResultType_,
      Expression * elseExpr_,
      Reference* elseResultType_,
      Reference* commonSupertype_) :
    conditionExpr(conditionExpr_),
    thenExpr(thenExpr_),
    thenResultType(thenResultType_),
    elseExpr(elseExpr_),
    elseResultType(elseResultType_),
    commonSupertype(commonSupertype_) {}
};

class While : public Expression {
public:
  Expression* bodyExpr;

  While(Expression* bodyExpr_) : bodyExpr(bodyExpr_) {}
};

class Consecutor : public Expression {
public:
  std::vector<Expression *> exprs;

  Consecutor(
      std::vector<Expression *> exprs_) :
      exprs(exprs_) {}
};

class Block : public Expression {
public:
  Expression * inner;
  Reference* innerType;

  Block(Expression * inner_, Reference* innerType_) :
  inner(inner_),
  innerType(innerType_) {}
};

class Return : public Expression {
public:
  Expression *sourceExpr;
  Reference* sourceType;

  Return(
    Expression *sourceExpr_,
    Reference* sourceType_)
    : sourceExpr(sourceExpr_),
      sourceType(sourceType_) {}
};


class ConstructUnknownSizeArray : public Expression {
public:
  Expression* sizeExpr;
  Reference* sizeType;
  Referend* sizeReferend;
  Expression* generatorExpr;
  Reference* generatorType;
  Referend* generatorReferend;
  Prototype* generatorMethod;
  bool generatorKnownLive;
  Reference* arrayRefType;
  Reference* elementType;

  ConstructUnknownSizeArray(
      Expression* sizeExpr_,
      Reference* sizeType_,
      Referend* sizeReferend_,
      Expression* generatorExpr_,
      Reference* generatorType_,
      Referend* generatorReferend_,
      Prototype* generatorMethod_,
      bool generatorKnownLive_,
      Reference* arrayRefType_,
      Reference* elementType_) :
    sizeExpr(sizeExpr_),
    sizeType(sizeType_),
    sizeReferend(sizeReferend_),
    generatorExpr(generatorExpr_),
    generatorType(generatorType_),
    generatorReferend(generatorReferend_),
    generatorMethod(generatorMethod_),
    generatorKnownLive(generatorKnownLive_),
    arrayRefType(arrayRefType_),
    elementType(elementType_) {}
};

class StaticArrayFromCallable : public Expression {
public:
  Expression* generatorExpr;
  Reference* generatorType;
  Referend* generatorReferend;
  Prototype* generatorMethod;
  bool generatorKnownLive;
  Reference* arrayRefType;
  Reference* elementType;

  StaticArrayFromCallable(
      Expression* generatorExpr_,
      Reference* generatorType_,
      Referend* generatorReferend_,
      Prototype* generatorMethod_,
      bool generatorKnownLive_,
      Reference* arrayRefType_,
      Reference* elementType_) :
      generatorExpr(generatorExpr_),
      generatorType(generatorType_),
      generatorReferend(generatorReferend_),
      generatorMethod(generatorMethod_),
      generatorKnownLive(generatorKnownLive_),
      arrayRefType(arrayRefType_),
      elementType(elementType_) {}
};

class DestroyKnownSizeArrayIntoFunction : public Expression {
public:
  Expression* arrayExpr;
  Reference* arrayType;
  KnownSizeArrayT* arrayReferend;
  Expression* consumerExpr;
  Reference* consumerType;
  Prototype* consumerMethod;
  bool consumerKnownLive;
  Reference* elementType;
  int arraySize;

  DestroyKnownSizeArrayIntoFunction(
      Expression* arrayExpr_,
      Reference* arrayType_,
      KnownSizeArrayT* arrayReferend_,
      Expression* consumerExpr_,
      Reference* consumerType_,
      Prototype* consumerMethod_,
      bool consumerKnownLive_,
      Reference* elementType_,
      int arraySize_) :
    arrayExpr(arrayExpr_),
    arrayType(arrayType_),
    arrayReferend(arrayReferend_),
    consumerExpr(consumerExpr_),
    consumerType(consumerType_),
    consumerMethod(consumerMethod_),
    consumerKnownLive(consumerKnownLive_),
    elementType(elementType_),
    arraySize(arraySize_) {}
};

class DestroyKnownSizeArrayIntoLocals : public Expression {
public:
  Expression* arrayExpr;
  Expression* consumerExpr;
};

class DestroyUnknownSizeArray : public Expression {
public:
  Expression* arrayExpr;
  Reference* arrayType;
  UnknownSizeArrayT* arrayReferend;
  Expression* consumerExpr;
  Reference* consumerType;
  InterfaceReferend* consumerReferend;
  Prototype* consumerMethod;
  bool consumerKnownLive;

  DestroyUnknownSizeArray(
      Expression* arrayExpr_,
      Reference* arrayType_,
      UnknownSizeArrayT* arrayReferend_,
      Expression* consumerExpr_,
      Reference* consumerType_,
      InterfaceReferend* consumerReferend_,
      Prototype* consumerMethod_,
      bool consumerKnownLive_) :
    arrayExpr(arrayExpr_),
    arrayType(arrayType_),
    arrayReferend(arrayReferend_),
    consumerExpr(consumerExpr_),
    consumerType(consumerType_),
    consumerReferend(consumerReferend_),
    consumerMethod(consumerMethod_),
    consumerKnownLive(consumerKnownLive_) {}
};

class NewStruct : public Expression {
public:
  std::vector<Expression*> sourceExprs;
  Reference* resultType;

  NewStruct(
      std::vector<Expression*> sourceExprs_,
      Reference* resultType_) :
      sourceExprs(sourceExprs_),
      resultType(resultType_) {}
};

class ArrayLength : public Expression {
public:
  Expression* sourceExpr;
  Reference* sourceType;
  bool sourceKnownLive;

  ArrayLength(
      Expression* sourceExpr_,
      Reference* sourceType_,
      bool sourceKnownLive_) :
      sourceExpr(sourceExpr_),
      sourceType(sourceType_),
      sourceKnownLive(sourceKnownLive_) {}
};


class CheckRefCount : public Expression {
public:
  Expression* refExpr;
  RefCountCategory category;
  Expression* numExpr;
};


class Discard : public Expression {
public:
  Expression* sourceExpr;
  Reference* sourceResultType;

  Discard(Expression* sourceExpr_, Reference* sourceResultType_) :
      sourceExpr(sourceExpr_), sourceResultType(sourceResultType_) {}
};


class LockWeak : public Expression {
public:
  Expression* sourceExpr;
  Reference* sourceType;
  bool sourceKnownLive;

  Prototype* someConstructor;
  Reference* someType;
  StructReferend* someReferend;

  Prototype* noneConstructor;
  Reference* noneType;
  StructReferend* noneReferend;

  Reference* resultOptType;
  InterfaceReferend* resultOptReferend;

  LockWeak(
      Expression* sourceExpr_,
      Reference* sourceType_,
      bool sourceKnownLive_,
      Prototype* someConstructor_,
      Reference* someType_,
      StructReferend* someReferend_,
      Prototype* noneConstructor_,
      Reference* noneType_,
      StructReferend* noneReferend_,
      Reference* resultOptType_,
      InterfaceReferend* resultOptReferend_) :
    sourceExpr(sourceExpr_),
    sourceType(sourceType_),
    sourceKnownLive(sourceKnownLive_),
    someConstructor(someConstructor_),
    someType(someType_),
    someReferend(someReferend_),
    noneConstructor(noneConstructor_),
    noneType(noneType_),
    noneReferend(noneReferend_),
    resultOptType(resultOptType_),
    resultOptReferend(resultOptReferend_) {}
};

class AsSubtype : public Expression {
public:
  Expression* sourceExpr;
  Reference* sourceType;
  bool sourceKnownLive;

  Referend* targetReferend;

  Prototype* someConstructor;
  Reference* someType;
  StructReferend* someReferend;

  Prototype* noneConstructor;
  Reference* noneType;
  StructReferend* noneReferend;

  Reference* resultOptType;
  InterfaceReferend* resultOptReferend;

  AsSubtype(
      Expression* sourceExpr_,
      Reference* sourceType_,
      bool sourceKnownLive_,
      Referend* targetReferend_,
      Prototype* someConstructor_,
      Reference* someType_,
      StructReferend* someReferend_,
      Prototype* noneConstructor_,
      Reference* noneType_,
      StructReferend* noneReferend_,
      Reference* resultOptType_,
      InterfaceReferend* resultOptReferend_) :
    sourceExpr(sourceExpr_),
    sourceType(sourceType_),
    sourceKnownLive(sourceKnownLive_),
    targetReferend(targetReferend_),
    someConstructor(someConstructor_),
    someType(someType_),
    someReferend(someReferend_),
    noneConstructor(noneConstructor_),
    noneType(noneType_),
    noneReferend(noneReferend_),
    resultOptType(resultOptType_),
    resultOptReferend(resultOptReferend_) {}
};

#endif