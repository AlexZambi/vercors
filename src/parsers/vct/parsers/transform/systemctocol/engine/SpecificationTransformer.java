package vct.parsers.transform.systemctocol.engine;

import javax.sound.sampled.Line;

import de.tub.pes.syscir.sc_model.SCVariable;
import de.tub.pes.syscir.sc_model.variables.SCArray;
import de.tub.pes.syscir.sc_model.variables.SCClassInstance;
import scala.Option;
import scala.reflect.ClassTag$;
import vct.col.ast.*;
import vct.col.ref.DirectRef;
import vct.col.ref.LazyRef;
import vct.col.ref.Ref;
import vct.parsers.transform.systemctocol.colmodel.COLClass;
import vct.parsers.transform.systemctocol.colmodel.COLSystem;
import vct.parsers.transform.systemctocol.colmodel.ProcessClass;
import vct.parsers.transform.systemctocol.engine.VariableTransformer;
import vct.parsers.transform.systemctocol.engine.ExpressionTransformer;
import de.tub.pes.syscir.sc_model.expressions.*;
import vct.parsers.transform.systemctocol.util.*;
import scala.math.BigInt;

/**
 * Creates specifications for methods and loops encountered in the SystemC system.
 *
 * @param <T> IGNORED
 */
public class SpecificationTransformer<T> {

    /**
     * Intermediate representation class that the method or loop to be specified is contained in.
     */
    private final COLClass col_class;

    /**
     * COL system context.
     */
    private final COLSystem<T> col_system;

    /**
     * Main reference field of the containing class.
     */
    private final InstanceField<T> m;

    public SpecificationTransformer(COLClass col_class, COLSystem<T> col_system, InstanceField<T> m) {
        this.col_class = col_class;
        this.col_system = col_system;
        this.m = m;
    }

    /**
     * Creates a loop invariant that propagates all permissions needed for basic verification. Also incorporates the
     * given path condition.
     *
     * @param path_condition Path condition at this loop
     * @return A basic loop invariant
     */
    public LoopInvariant<T> create_loop_invariant(Expr<T> path_condition) {
        return new LoopInvariant<>(create_basic_invariant(path_condition), Option.empty(), new GeneratedBlame<>(), OriGen.create());
    }

    /**
     * Generate loop invariant for a for loop.
     *
     * @param expr Expression of the for loop
     * @param sc_inst SystemC class instance that this expression refers to
     * @param obj Object representing the current access depth up to and including <code>sc_inst</code>
     * @param path_cond Path condition at this statement (only used for the first expression of a block)
     * @return A loop invariant that contains at least permissions needed for basic verification, the path condition, 
     * and possibly generated loop bounds.
     */
    public LoopInvariant<T> create_for_loop_invariant(ForLoopExpression expr, SCClassInstance sc_inst, Expr<T> obj, Expr<T> path_cond) {
        BinaryExpression initializer = (BinaryExpression) expr.getInitializer();
        BinaryExpression condition = (BinaryExpression) expr.getCondition();
        BinaryExpression iterator = (BinaryExpression) expr.getIterator();
        java.util.List<Expression> body = expr.getLoopBody();

        InvariantGenerator<T> invariant_generator = new InvariantGenerator<>(col_system);
        LinearExpression init_var = invariant_generator.getLinearExpression(initializer.getLeft(), sc_inst);
        LinearExpression init_value = invariant_generator.getLinearExpression(initializer.getRight(), sc_inst);

        LinearExpression transfer_var = invariant_generator.getLinearExpression(iterator.getLeft(), sc_inst);
        LinearExpression transfer_function = invariant_generator.getLinearExpression(iterator.getRight(), sc_inst);
        Compare cond = invariant_generator.getCompare(condition, sc_inst);

        if (init_var == null ||
            init_value == null ||
            transfer_var == null ||
            transfer_function == null ||
            cond == null || 
            cond.getOp() == ComparisonType.EQ || 
            cond.getOp() == ComparisonType.NEQ) {

            return this.create_loop_invariant(path_cond);
        }

        // we consider only the case when there is only one counter variable
        if (!init_var.isUnivariate() || !cond.getLeft().isUnivariate()) {
            return this.create_loop_invariant(path_cond);
        }
        Object init_variable = init_var.getVariable();
        Object cond_variable = cond.getLeft().getVariable();
        if (!init_variable.equals(cond_variable)) {
            return this.create_loop_invariant(path_cond);
        }
        Expr<T> var = (Expr<T>) init_variable;

        SymbolicState state = invariant_generator.parentToState(expr, sc_inst);
        // also add the counter variable
        state.add(init_variable, new SymbolicInterval(init_value));
        state = state.reduce();

        System.out.println(state);
        System.out.println();
        System.out.println();
        System.out.println();

        java.util.List<Assignment> parsed_body = invariant_generator.parseBody(body, sc_inst);
        // also add the loop iterator as a final assignment in the body
        parsed_body.add(new Assignment(var, transfer_function));

        Expr<T> bound_invariant = invariant_generator.generateInvariant(init_value, cond, transfer_function, var);
        Expr<T> symbolic_invariant = invariant_generator.generateInvariant(state, parsed_body, cond);

        if (symbolic_invariant != null) {
            bound_invariant = symbolic_invariant;
        }

        if (bound_invariant != null) {
            return new LoopInvariant<>(col_system.fold_star(java.util.List.of(this.create_basic_invariant(path_cond), bound_invariant)), Option.empty(), new GeneratedBlame<>(), OriGen.create());
        }
        return this.create_loop_invariant(path_cond);
    }

    /**
     * Creates the contract of a regular method, i.e. one that requires (and ensures) that <code>m</code> is locked
     * before execution.
     *
     * @return A contract for a regular method
     */
    public ApplicableContract<T> create_basic_method_contract() {
        Expr<T> context = create_basic_invariant(col_system.TRUE);
        return col_system.to_applicable_contract(context, context);
    }

    /**
     * Creates the contract for a run method, i.e. one that locks <code>m</code> itself.
     *
     * @return A contract for a run method
     */
    public ApplicableContract<T> create_run_method_contract() {
        Expr<T> context = create_run_method_invariant();
        return col_system.to_applicable_contract(context, context);
    }

    /**
     * Creates the contract for a constructor. The contract ensures permission to all fields and that the Main reference
     * matches the parameter that sets it.
     *
     * @param fields A map from SystemC variables to COL fields representing the fields that the constructor should
     *               return permission to
     * @param m_param Parameter that should match the Main reference <code>m</code>
     * @return A contract for a constructor
     */
    public ApplicableContract<T> create_constructor_contract(java.util.Map<SCVariable, InstanceField<T>> fields, Variable<T> m_param) {
        // Get references to m and m_param
        Ref<T, InstanceField<T>> m_ref = new DirectRef<>(m, ClassTag$.MODULE$.apply(InstanceField.class));
        Deref<T> m_deref = new Deref<>(col_system.THIS, m_ref, new GeneratedBlame<>(), OriGen.create());
        FieldLocation<T> m_loc = new FieldLocation<>(col_system.THIS, m_ref, OriGen.create());
        Local<T> m_param_local = new Local<>(new DirectRef<>(m_param, ClassTag$.MODULE$.apply(Variable.class)), OriGen.create());

        // Always return permission to m and that m is set by m_param
        java.util.List<Expr<T>> conds = new java.util.ArrayList<>();
        conds.add(new Perm<>(m_loc, new WritePerm<>(OriGen.create()), OriGen.create()));
        conds.add(new Eq<>(m_deref, m_param_local, OriGen.create()));

        // Add permissions and functional properties about all other fields as well
        for (java.util.Map.Entry<SCVariable, InstanceField<T>> translation : fields.entrySet()) {
            // Unpack entry
            SCVariable sc_var = translation.getKey();
            InstanceField<T> field = translation.getValue();

            // Get references to the field
            Ref<T, InstanceField<T>> field_ref = new DirectRef<>(field, ClassTag$.MODULE$.apply(InstanceField.class));
            Deref<T> field_deref = new Deref<>(col_system.THIS, field_ref, new GeneratedBlame<>(), OriGen.create());
            FieldLocation<T> field_loc = new FieldLocation<>(col_system.THIS, field_ref, OriGen.create());

            // If the variable is an array, try to return array specifications. Also, only return read permission to the
            // array field itself
            if (sc_var instanceof SCArray sc_arr) {
                conds.add(new Perm<>(field_loc, new ReadPerm<>(OriGen.create()), OriGen.create()));
                conds.addAll(col_system.get_array_specifications(sc_arr, field_deref, m_deref));
            }
            // For all other fields, return write permission
            else {
                conds.add(new Perm<>(field_loc, new WritePerm<>(OriGen.create()), OriGen.create()));
            }
        }

        if (col_class instanceof ProcessClass) conds.add(new IdleToken<>(col_system.THIS, OriGen.create()));

        // TODO: Add specifications for variables that are set by parameters or by constants

        // Convert expression list to applicable contract and return
        Expr<T> ensures = col_system.fold_star(conds);
        return col_system.to_applicable_contract(col_system.TRUE, ensures);
    }

    /**
     * Helper method that creates a basic permission invariant for situations in which the global lock is held and the
     * lock invariant can be asserted.
     *
     * @param path_condition Path condition at this position
     * @return An invariant (for both simple methods and loops) that grants permission to all relevant objects
     */
    private Expr<T> create_basic_invariant(Expr<T> path_condition) {
        // Create references to m
        Ref<T, InstanceField<T>> m_ref = new DirectRef<>(m, ClassTag$.MODULE$.apply(InstanceField.class));
        Deref<T> m_deref = new Deref<>(col_system.THIS, m_ref, new GeneratedBlame<>(), OriGen.create());
        FieldLocation<T> m_loc = new FieldLocation<>(col_system.THIS, m_ref, OriGen.create());

        // Create reference to this class's instance in the Main class
        Ref<T, InstanceField<T>> this_ref = new LazyRef<>(() -> col_system.get_instance_by_class(col_class), Option.empty(),
                ClassTag$.MODULE$.apply(InstanceField.class));
        Deref<T> this_deref = new Deref<>(m_deref, this_ref, new GeneratedBlame<>(), OriGen.create());

        // Create the individual conditions
        Perm<T> m_perm = new Perm<>(m_loc, get_permission_to_m(), OriGen.create());
        Neq<T> m_not_null = new Neq<>(m_deref, col_system.NULL, OriGen.create());
        Committed<T> m_committed = new Committed<>(m_deref, new GeneratedBlame<>(), OriGen.create());
        Held<T> m_held = new Held<>(m_deref, OriGen.create());
        Ref<T, InstancePredicate<T>> global_inv_ref = new LazyRef<>(col_system::get_global_perms, Option.empty(),
                ClassTag$.MODULE$.apply(InstancePredicate.class));
        Expr<T> global_inv = col_system.fold_preds(new InstancePredicateApply<>(m_deref, global_inv_ref,
                col_system.NO_EXPRS, OriGen.create()));
        Eq<T> this_this = new Eq<>(this_deref, col_system.THIS, OriGen.create());

        // Put everything together and return the condition
        if (path_condition.equals(col_system.TRUE)) {
            return col_system.fold_star(java.util.List.of(m_perm, m_not_null, m_committed, m_held, global_inv, this_this));
        }
        else {
            return col_system.fold_star(java.util.List.of(m_perm, m_not_null, m_committed, m_held, global_inv, this_this, path_condition));
        }
    }

    /**
     * Helper method that creates a contract for a run method without the ability to assert the global permission
     * invariant.
     *
     * @return An invariant for a run method that grants the necessary permissions to lock on <code>m</code>
     */
    private Expr<T> create_run_method_invariant() {
        // Create references to m
        Ref<T, InstanceField<T>> m_ref = new DirectRef<>(m, ClassTag$.MODULE$.apply(InstanceField.class));
        Deref<T> m_deref = new Deref<>(col_system.THIS, m_ref, new GeneratedBlame<>(), OriGen.create());
        FieldLocation<T> m_loc = new FieldLocation<>(col_system.THIS, m_ref, OriGen.create());

        // Create references to this class's instance in the Main class
        Ref<T, InstanceField<T>> this_ref = new LazyRef<>(() -> col_system.get_instance_by_class(col_class), Option.empty(),
                ClassTag$.MODULE$.apply(InstanceField.class));
        Deref<T> this_deref = new Deref<>(m_deref, this_ref, new GeneratedBlame<>(), OriGen.create());
        FieldLocation<T> this_loc = new FieldLocation<>(m_deref, this_ref, OriGen.create());

        // Create the individual conditions
        Perm<T> m_perm = new Perm<>(m_loc, get_permission_to_m(), OriGen.create());
        Neq<T> m_not_null = new Neq<>(m_deref, col_system.NULL, OriGen.create());
        Committed<T> m_committed = new Committed<>(m_deref, new GeneratedBlame<>(), OriGen.create());
        Perm<T> this_perm = new Perm<>(this_loc, new ReadPerm<>(OriGen.create()), OriGen.create());
        Eq<T> this_this = new Eq<>(this_deref, col_system.THIS, OriGen.create());

        // Put everything together and return the condition
        return col_system.fold_star(java.util.List.of(m_perm, m_not_null, m_committed, this_perm, this_this));
    }

    /**
     * Helper function that determines how much permission one should have to <code>m</code>. Processes should have 1\2
     * permission (as read permission might cause the generation of an existential quantifier in Silicon, causing an
     * obscure error), other classes should have read permission.
     *
     * @return 1\2 if the containing class is a process, read otherwise
     */
    private Expr<T> get_permission_to_m() {
        if (col_class instanceof ProcessClass) return col_system.HALF;
        else return new ReadPerm<>(OriGen.create());
    }
}
