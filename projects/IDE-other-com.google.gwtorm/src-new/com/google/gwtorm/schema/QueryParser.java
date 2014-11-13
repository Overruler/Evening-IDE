// $ANTLR 3.1.1 C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g 2014-10-16 21:01:19

package com.google.gwtorm.schema;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;


import org.antlr.runtime.tree.*;

public class QueryParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "WHERE", "ORDER", "BY", "AND", "LT", "LE", "GT", "GE", "EQ", "ID", "PLACEHOLDER", "COMMA", "ASC", "DESC", "LIMIT", "CONSTANT_INTEGER", "CONSTANT_STRING", "TRUE", "FALSE", "DOT", "WS"
    };
    public static final int LT=8;
    public static final int LIMIT=18;
    public static final int DESC=17;
    public static final int ORDER=5;
    public static final int ASC=16;
    public static final int BY=6;
    public static final int ID=13;
    public static final int WS=24;
    public static final int EOF=-1;
    public static final int GE=11;
    public static final int COMMA=15;
    public static final int DOT=23;
    public static final int TRUE=21;
    public static final int CONSTANT_STRING=20;
    public static final int WHERE=4;
    public static final int EQ=12;
    public static final int GT=10;
    public static final int PLACEHOLDER=14;
    public static final int CONSTANT_INTEGER=19;
    public static final int AND=7;
    public static final int LE=9;
    public static final int FALSE=22;

    // delegates
    // delegators


        public QueryParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public QueryParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        
    protected TreeAdaptor adaptor = new CommonTreeAdaptor();

    public void setTreeAdaptor(TreeAdaptor adaptor) {
        this.adaptor = adaptor;
    }
    public TreeAdaptor getTreeAdaptor() {
        return adaptor;
    }

    public String[] getTokenNames() { return QueryParser.tokenNames; }
    public String getGrammarFileName() { return "C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g"; }


        public static Tree parse(final RelationModel m, final String str)
          throws QueryParseException {
          try {
            final QueryParser p = new QueryParser(
              new TokenRewriteStream(
                new QueryLexer(
                  new ANTLRStringStream(str)
                )
              )
            );
            p.relationModel = m;
            return (Tree)p.query().getTree();
          } catch (QueryParseInternalException e) {
            throw new QueryParseException(e.getMessage());
          } catch (RecognitionException e) {
            throw new QueryParseException(e.getMessage());
          }
        }

        public static class Column extends CommonTree {
          private static ColumnModel resolve(Tree node, RelationModel model) {
            ColumnModel c;
            if (node.getType() == ID) {
              c = model.getField(node.getText());
            } else {
              c = resolve(node.getChild(0), model);
            }
            if (c == null) {
              throw new QueryParseInternalException("No field " + node.getText());
            }
            if (node.getType() == DOT) {
              c = resolve(node.getChild(1), c);
            }
            return c;
          }

          private static ColumnModel resolve(Tree node, ColumnModel model) {
            ColumnModel c;
            if (node.getType() == ID) {
              c = model.getField(node.getText());
            } else {
              c = resolve(node.getChild(0), model);
            }
            if (c == null) {
              throw new QueryParseInternalException("No field " + node.getText());
            }
            if (node.getType() == DOT) {
              c = resolve(node.getChild(1), c);
            }
            return c;
          }

          private final ColumnModel field;

          public Column(int ttype, Tree tree, final RelationModel relationModel) {
            field = resolve(tree, relationModel);
            token = new CommonToken(ID, field.getPathToFieldName());
          }

          public Column(final Column o, final ColumnModel f) {
            token = o.token;
            field = f;
          }

          public ColumnModel getField() {
            return field;
          }

          public Tree dupNode() {
            return new Column(this, field);
          }
        }

        private RelationModel relationModel;

        public void displayRecognitionError(String[] tokenNames,
                                            RecognitionException e) {
            String hdr = getErrorHeader(e);
            String msg = getErrorMessage(e, tokenNames);
            throw new QueryParseInternalException(hdr + " " + msg);
        }


    public static class query_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "query"
    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:143:1: query : ( where )? ( orderBy )? ( limit )? ;
    public final QueryParser.query_return query() throws RecognitionException {
        QueryParser.query_return retval = new QueryParser.query_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        QueryParser.where_return where1 = null;

        QueryParser.orderBy_return orderBy2 = null;

        QueryParser.limit_return limit3 = null;



        try {
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:144:3: ( ( where )? ( orderBy )? ( limit )? )
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:144:5: ( where )? ( orderBy )? ( limit )?
            {
            root_0 = (Object)adaptor.nil();

            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:144:5: ( where )?
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0==WHERE) ) {
                alt1=1;
            }
            switch (alt1) {
                case 1 :
                    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:144:5: where
                    {
                    pushFollow(FOLLOW_where_in_query177);
                    where1=where();

                    state._fsp--;

                    adaptor.addChild(root_0, where1.getTree());

                    }
                    break;

            }

            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:144:12: ( orderBy )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==ORDER) ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:144:12: orderBy
                    {
                    pushFollow(FOLLOW_orderBy_in_query180);
                    orderBy2=orderBy();

                    state._fsp--;

                    adaptor.addChild(root_0, orderBy2.getTree());

                    }
                    break;

            }

            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:144:21: ( limit )?
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==LIMIT) ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:144:21: limit
                    {
                    pushFollow(FOLLOW_limit_in_query183);
                    limit3=limit();

                    state._fsp--;

                    adaptor.addChild(root_0, limit3.getTree());

                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "query"

    public static class where_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "where"
    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:147:1: where : WHERE conditions ;
    public final QueryParser.where_return where() throws RecognitionException {
        QueryParser.where_return retval = new QueryParser.where_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token WHERE4=null;
        QueryParser.conditions_return conditions5 = null;


        Object WHERE4_tree=null;

        try {
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:148:3: ( WHERE conditions )
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:148:5: WHERE conditions
            {
            root_0 = (Object)adaptor.nil();

            WHERE4=(Token)match(input,WHERE,FOLLOW_WHERE_in_where197); 
            WHERE4_tree = (Object)adaptor.create(WHERE4);
            root_0 = (Object)adaptor.becomeRoot(WHERE4_tree, root_0);

            pushFollow(FOLLOW_conditions_in_where200);
            conditions5=conditions();

            state._fsp--;

            adaptor.addChild(root_0, conditions5.getTree());

            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "where"

    public static class orderBy_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "orderBy"
    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:151:1: orderBy : ORDER BY fieldSort ( COMMA fieldSort )* ;
    public final QueryParser.orderBy_return orderBy() throws RecognitionException {
        QueryParser.orderBy_return retval = new QueryParser.orderBy_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ORDER6=null;
        Token BY7=null;
        Token COMMA9=null;
        QueryParser.fieldSort_return fieldSort8 = null;

        QueryParser.fieldSort_return fieldSort10 = null;


        Object ORDER6_tree=null;
        Object BY7_tree=null;
        Object COMMA9_tree=null;

        try {
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:152:3: ( ORDER BY fieldSort ( COMMA fieldSort )* )
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:152:5: ORDER BY fieldSort ( COMMA fieldSort )*
            {
            root_0 = (Object)adaptor.nil();

            ORDER6=(Token)match(input,ORDER,FOLLOW_ORDER_in_orderBy213); 
            ORDER6_tree = (Object)adaptor.create(ORDER6);
            root_0 = (Object)adaptor.becomeRoot(ORDER6_tree, root_0);

            BY7=(Token)match(input,BY,FOLLOW_BY_in_orderBy216); 
            pushFollow(FOLLOW_fieldSort_in_orderBy219);
            fieldSort8=fieldSort();

            state._fsp--;

            adaptor.addChild(root_0, fieldSort8.getTree());
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:152:26: ( COMMA fieldSort )*
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);

                if ( (LA4_0==COMMA) ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:152:27: COMMA fieldSort
            	    {
            	    COMMA9=(Token)match(input,COMMA,FOLLOW_COMMA_in_orderBy222); 
            	    pushFollow(FOLLOW_fieldSort_in_orderBy225);
            	    fieldSort10=fieldSort();

            	    state._fsp--;

            	    adaptor.addChild(root_0, fieldSort10.getTree());

            	    }
            	    break;

            	default :
            	    break loop4;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "orderBy"

    public static class fieldSort_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "fieldSort"
    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:155:1: fieldSort : ( field sortDirection | field -> ^( ASC field ) );
    public final QueryParser.fieldSort_return fieldSort() throws RecognitionException {
        QueryParser.fieldSort_return retval = new QueryParser.fieldSort_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        QueryParser.field_return field11 = null;

        QueryParser.sortDirection_return sortDirection12 = null;

        QueryParser.field_return field13 = null;


        RewriteRuleSubtreeStream stream_field=new RewriteRuleSubtreeStream(adaptor,"rule field");
        try {
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:156:3: ( field sortDirection | field -> ^( ASC field ) )
            int alt5=2;
            alt5 = dfa5.predict(input);
            switch (alt5) {
                case 1 :
                    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:156:5: field sortDirection
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_field_in_fieldSort240);
                    field11=field();

                    state._fsp--;

                    adaptor.addChild(root_0, field11.getTree());
                    pushFollow(FOLLOW_sortDirection_in_fieldSort242);
                    sortDirection12=sortDirection();

                    state._fsp--;

                    root_0 = (Object)adaptor.becomeRoot(sortDirection12.getTree(), root_0);

                    }
                    break;
                case 2 :
                    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:157:5: field
                    {
                    pushFollow(FOLLOW_field_in_fieldSort249);
                    field13=field();

                    state._fsp--;

                    stream_field.add(field13.getTree());


                    // AST REWRITE
                    // elements: field
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 157:11: -> ^( ASC field )
                    {
                        // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:157:14: ^( ASC field )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot((Object)adaptor.create(ASC, "ASC"), root_1);

                        adaptor.addChild(root_1, stream_field.nextTree());

                        adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                    }
                    break;

            }
            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "fieldSort"

    public static class sortDirection_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "sortDirection"
    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:160:1: sortDirection : ( ASC | DESC );
    public final QueryParser.sortDirection_return sortDirection() throws RecognitionException {
        QueryParser.sortDirection_return retval = new QueryParser.sortDirection_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set14=null;

        Object set14_tree=null;

        try {
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:161:3: ( ASC | DESC )
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:
            {
            root_0 = (Object)adaptor.nil();

            set14=(Token)input.LT(1);
            if ( (input.LA(1)>=ASC && input.LA(1)<=DESC) ) {
                input.consume();
                adaptor.addChild(root_0, (Object)adaptor.create(set14));
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "sortDirection"

    public static class limit_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "limit"
    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:165:1: limit : LIMIT limitArg ;
    public final QueryParser.limit_return limit() throws RecognitionException {
        QueryParser.limit_return retval = new QueryParser.limit_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token LIMIT15=null;
        QueryParser.limitArg_return limitArg16 = null;


        Object LIMIT15_tree=null;

        try {
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:166:3: ( LIMIT limitArg )
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:166:5: LIMIT limitArg
            {
            root_0 = (Object)adaptor.nil();

            LIMIT15=(Token)match(input,LIMIT,FOLLOW_LIMIT_in_limit289); 
            LIMIT15_tree = (Object)adaptor.create(LIMIT15);
            root_0 = (Object)adaptor.becomeRoot(LIMIT15_tree, root_0);

            pushFollow(FOLLOW_limitArg_in_limit292);
            limitArg16=limitArg();

            state._fsp--;

            adaptor.addChild(root_0, limitArg16.getTree());

            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "limit"

    public static class limitArg_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "limitArg"
    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:169:1: limitArg : ( PLACEHOLDER | CONSTANT_INTEGER );
    public final QueryParser.limitArg_return limitArg() throws RecognitionException {
        QueryParser.limitArg_return retval = new QueryParser.limitArg_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set17=null;

        Object set17_tree=null;

        try {
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:170:3: ( PLACEHOLDER | CONSTANT_INTEGER )
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:
            {
            root_0 = (Object)adaptor.nil();

            set17=(Token)input.LT(1);
            if ( input.LA(1)==PLACEHOLDER||input.LA(1)==CONSTANT_INTEGER ) {
                input.consume();
                adaptor.addChild(root_0, (Object)adaptor.create(set17));
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "limitArg"

    public static class conditions_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "conditions"
    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:174:1: conditions : ( condition AND condition ( AND condition )* | condition );
    public final QueryParser.conditions_return conditions() throws RecognitionException {
        QueryParser.conditions_return retval = new QueryParser.conditions_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token AND19=null;
        Token AND21=null;
        QueryParser.condition_return condition18 = null;

        QueryParser.condition_return condition20 = null;

        QueryParser.condition_return condition22 = null;

        QueryParser.condition_return condition23 = null;


        Object AND19_tree=null;
        Object AND21_tree=null;

        try {
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:175:3: ( condition AND condition ( AND condition )* | condition )
            int alt7=2;
            alt7 = dfa7.predict(input);
            switch (alt7) {
                case 1 :
                    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:175:5: condition AND condition ( AND condition )*
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_condition_in_conditions324);
                    condition18=condition();

                    state._fsp--;

                    adaptor.addChild(root_0, condition18.getTree());
                    AND19=(Token)match(input,AND,FOLLOW_AND_in_conditions326); 
                    AND19_tree = (Object)adaptor.create(AND19);
                    root_0 = (Object)adaptor.becomeRoot(AND19_tree, root_0);

                    pushFollow(FOLLOW_condition_in_conditions329);
                    condition20=condition();

                    state._fsp--;

                    adaptor.addChild(root_0, condition20.getTree());
                    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:175:30: ( AND condition )*
                    loop6:
                    do {
                        int alt6=2;
                        int LA6_0 = input.LA(1);

                        if ( (LA6_0==AND) ) {
                            alt6=1;
                        }


                        switch (alt6) {
                    	case 1 :
                    	    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:175:31: AND condition
                    	    {
                    	    AND21=(Token)match(input,AND,FOLLOW_AND_in_conditions332); 
                    	    pushFollow(FOLLOW_condition_in_conditions335);
                    	    condition22=condition();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, condition22.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop6;
                        }
                    } while (true);


                    }
                    break;
                case 2 :
                    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:176:5: condition
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_condition_in_conditions343);
                    condition23=condition();

                    state._fsp--;

                    adaptor.addChild(root_0, condition23.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "conditions"

    public static class condition_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "condition"
    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:179:1: condition : field compare_op conditionValue ;
    public final QueryParser.condition_return condition() throws RecognitionException {
        QueryParser.condition_return retval = new QueryParser.condition_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        QueryParser.field_return field24 = null;

        QueryParser.compare_op_return compare_op25 = null;

        QueryParser.conditionValue_return conditionValue26 = null;



        try {
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:180:3: ( field compare_op conditionValue )
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:180:5: field compare_op conditionValue
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_field_in_condition356);
            field24=field();

            state._fsp--;

            adaptor.addChild(root_0, field24.getTree());
            pushFollow(FOLLOW_compare_op_in_condition358);
            compare_op25=compare_op();

            state._fsp--;

            root_0 = (Object)adaptor.becomeRoot(compare_op25.getTree(), root_0);
            pushFollow(FOLLOW_conditionValue_in_condition361);
            conditionValue26=conditionValue();

            state._fsp--;

            adaptor.addChild(root_0, conditionValue26.getTree());

            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "condition"

    public static class compare_op_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "compare_op"
    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:183:1: compare_op : ( LT | LE | GT | GE | EQ );
    public final QueryParser.compare_op_return compare_op() throws RecognitionException {
        QueryParser.compare_op_return retval = new QueryParser.compare_op_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set27=null;

        Object set27_tree=null;

        try {
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:184:2: ( LT | LE | GT | GE | EQ )
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:
            {
            root_0 = (Object)adaptor.nil();

            set27=(Token)input.LT(1);
            if ( (input.LA(1)>=LT && input.LA(1)<=EQ) ) {
                input.consume();
                adaptor.addChild(root_0, (Object)adaptor.create(set27));
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "compare_op"

    public static class field_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "field"
    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:191:1: field : n= qualifiedFieldName -> ID[(Tree)n.tree, relationModel] ;
    public final QueryParser.field_return field() throws RecognitionException {
        QueryParser.field_return retval = new QueryParser.field_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        QueryParser.qualifiedFieldName_return n = null;


        RewriteRuleSubtreeStream stream_qualifiedFieldName=new RewriteRuleSubtreeStream(adaptor,"rule qualifiedFieldName");
        try {
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:192:3: (n= qualifiedFieldName -> ID[(Tree)n.tree, relationModel] )
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:192:5: n= qualifiedFieldName
            {
            pushFollow(FOLLOW_qualifiedFieldName_in_field407);
            n=qualifiedFieldName();

            state._fsp--;

            stream_qualifiedFieldName.add(n.getTree());


            // AST REWRITE
            // elements: 
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"token retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 192:26: -> ID[(Tree)n.tree, relationModel]
            {
                adaptor.addChild(root_0, new Column(ID, (Tree)n.tree, relationModel));

            }

            retval.tree = root_0;
            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "field"

    public static class qualifiedFieldName_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "qualifiedFieldName"
    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:195:1: qualifiedFieldName : ID ( DOT ID )* ;
    public final QueryParser.qualifiedFieldName_return qualifiedFieldName() throws RecognitionException {
        QueryParser.qualifiedFieldName_return retval = new QueryParser.qualifiedFieldName_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token ID28=null;
        Token DOT29=null;
        Token ID30=null;

        Object ID28_tree=null;
        Object DOT29_tree=null;
        Object ID30_tree=null;

        try {
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:196:3: ( ID ( DOT ID )* )
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:196:5: ID ( DOT ID )*
            {
            root_0 = (Object)adaptor.nil();

            ID28=(Token)match(input,ID,FOLLOW_ID_in_qualifiedFieldName428); 
            ID28_tree = (Object)adaptor.create(ID28);
            adaptor.addChild(root_0, ID28_tree);

            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:196:8: ( DOT ID )*
            loop8:
            do {
                int alt8=2;
                int LA8_0 = input.LA(1);

                if ( (LA8_0==DOT) ) {
                    alt8=1;
                }


                switch (alt8) {
            	case 1 :
            	    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:196:9: DOT ID
            	    {
            	    DOT29=(Token)match(input,DOT,FOLLOW_DOT_in_qualifiedFieldName431); 
            	    DOT29_tree = (Object)adaptor.create(DOT29);
            	    root_0 = (Object)adaptor.becomeRoot(DOT29_tree, root_0);

            	    ID30=(Token)match(input,ID,FOLLOW_ID_in_qualifiedFieldName434); 
            	    ID30_tree = (Object)adaptor.create(ID30);
            	    adaptor.addChild(root_0, ID30_tree);


            	    }
            	    break;

            	default :
            	    break loop8;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "qualifiedFieldName"

    public static class conditionValue_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "conditionValue"
    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:199:1: conditionValue : ( PLACEHOLDER | CONSTANT_INTEGER | constantBoolean | CONSTANT_STRING );
    public final QueryParser.conditionValue_return conditionValue() throws RecognitionException {
        QueryParser.conditionValue_return retval = new QueryParser.conditionValue_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token PLACEHOLDER31=null;
        Token CONSTANT_INTEGER32=null;
        Token CONSTANT_STRING34=null;
        QueryParser.constantBoolean_return constantBoolean33 = null;


        Object PLACEHOLDER31_tree=null;
        Object CONSTANT_INTEGER32_tree=null;
        Object CONSTANT_STRING34_tree=null;

        try {
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:200:3: ( PLACEHOLDER | CONSTANT_INTEGER | constantBoolean | CONSTANT_STRING )
            int alt9=4;
            switch ( input.LA(1) ) {
            case PLACEHOLDER:
                {
                alt9=1;
                }
                break;
            case CONSTANT_INTEGER:
                {
                alt9=2;
                }
                break;
            case TRUE:
            case FALSE:
                {
                alt9=3;
                }
                break;
            case CONSTANT_STRING:
                {
                alt9=4;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 9, 0, input);

                throw nvae;
            }

            switch (alt9) {
                case 1 :
                    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:200:5: PLACEHOLDER
                    {
                    root_0 = (Object)adaptor.nil();

                    PLACEHOLDER31=(Token)match(input,PLACEHOLDER,FOLLOW_PLACEHOLDER_in_conditionValue449); 
                    PLACEHOLDER31_tree = (Object)adaptor.create(PLACEHOLDER31);
                    adaptor.addChild(root_0, PLACEHOLDER31_tree);


                    }
                    break;
                case 2 :
                    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:201:5: CONSTANT_INTEGER
                    {
                    root_0 = (Object)adaptor.nil();

                    CONSTANT_INTEGER32=(Token)match(input,CONSTANT_INTEGER,FOLLOW_CONSTANT_INTEGER_in_conditionValue455); 
                    CONSTANT_INTEGER32_tree = (Object)adaptor.create(CONSTANT_INTEGER32);
                    adaptor.addChild(root_0, CONSTANT_INTEGER32_tree);


                    }
                    break;
                case 3 :
                    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:202:5: constantBoolean
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_constantBoolean_in_conditionValue461);
                    constantBoolean33=constantBoolean();

                    state._fsp--;

                    adaptor.addChild(root_0, constantBoolean33.getTree());

                    }
                    break;
                case 4 :
                    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:203:5: CONSTANT_STRING
                    {
                    root_0 = (Object)adaptor.nil();

                    CONSTANT_STRING34=(Token)match(input,CONSTANT_STRING,FOLLOW_CONSTANT_STRING_in_conditionValue467); 
                    CONSTANT_STRING34_tree = (Object)adaptor.create(CONSTANT_STRING34);
                    adaptor.addChild(root_0, CONSTANT_STRING34_tree);


                    }
                    break;

            }
            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "conditionValue"

    public static class constantBoolean_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "constantBoolean"
    // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:206:1: constantBoolean : ( TRUE | FALSE );
    public final QueryParser.constantBoolean_return constantBoolean() throws RecognitionException {
        QueryParser.constantBoolean_return retval = new QueryParser.constantBoolean_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set35=null;

        Object set35_tree=null;

        try {
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:207:3: ( TRUE | FALSE )
            // C:\\Evening-IDE\\libraries\\google.gwtorm\\src\\main\\antlr\\com\\google\\gwtorm\\schema\\Query.g:
            {
            root_0 = (Object)adaptor.nil();

            set35=(Token)input.LT(1);
            if ( (input.LA(1)>=TRUE && input.LA(1)<=FALSE) ) {
                input.consume();
                adaptor.addChild(root_0, (Object)adaptor.create(set35));
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "constantBoolean"

    // Delegated rules


    protected DFA5 dfa5 = new DFA5(this);
    protected DFA7 dfa7 = new DFA7(this);
    static final String DFA5_eotS =
        "\6\uffff";
    static final String DFA5_eofS =
        "\1\uffff\1\4\3\uffff\1\4";
    static final String DFA5_minS =
        "\1\15\1\17\1\15\2\uffff\1\17";
    static final String DFA5_maxS =
        "\1\15\1\27\1\15\2\uffff\1\27";
    static final String DFA5_acceptS =
        "\3\uffff\1\1\1\2\1\uffff";
    static final String DFA5_specialS =
        "\6\uffff}>";
    static final String[] DFA5_transitionS = {
            "\1\1",
            "\1\4\2\3\1\4\4\uffff\1\2",
            "\1\5",
            "",
            "",
            "\1\4\2\3\1\4\4\uffff\1\2"
    };

    static final short[] DFA5_eot = DFA.unpackEncodedString(DFA5_eotS);
    static final short[] DFA5_eof = DFA.unpackEncodedString(DFA5_eofS);
    static final char[] DFA5_min = DFA.unpackEncodedStringToUnsignedChars(DFA5_minS);
    static final char[] DFA5_max = DFA.unpackEncodedStringToUnsignedChars(DFA5_maxS);
    static final short[] DFA5_accept = DFA.unpackEncodedString(DFA5_acceptS);
    static final short[] DFA5_special = DFA.unpackEncodedString(DFA5_specialS);
    static final short[][] DFA5_transition;

    static {
        int numStates = DFA5_transitionS.length;
        DFA5_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA5_transition[i] = DFA.unpackEncodedString(DFA5_transitionS[i]);
        }
    }

    class DFA5 extends DFA {

        public DFA5(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 5;
            this.eot = DFA5_eot;
            this.eof = DFA5_eof;
            this.min = DFA5_min;
            this.max = DFA5_max;
            this.accept = DFA5_accept;
            this.special = DFA5_special;
            this.transition = DFA5_transition;
        }
        public String getDescription() {
            return "155:1: fieldSort : ( field sortDirection | field -> ^( ASC field ) );";
        }
    }
    static final String DFA7_eotS =
        "\13\uffff";
    static final String DFA7_eofS =
        "\5\uffff\4\12\2\uffff";
    static final String DFA7_minS =
        "\1\15\1\10\1\15\1\16\1\10\4\5\2\uffff";
    static final String DFA7_maxS =
        "\1\15\1\27\1\15\1\26\1\27\4\22\2\uffff";
    static final String DFA7_acceptS =
        "\11\uffff\1\1\1\2";
    static final String DFA7_specialS =
        "\13\uffff}>";
    static final String[] DFA7_transitionS = {
            "\1\1",
            "\5\3\12\uffff\1\2",
            "\1\4",
            "\1\5\4\uffff\1\6\1\10\2\7",
            "\5\3\12\uffff\1\2",
            "\1\12\1\uffff\1\11\12\uffff\1\12",
            "\1\12\1\uffff\1\11\12\uffff\1\12",
            "\1\12\1\uffff\1\11\12\uffff\1\12",
            "\1\12\1\uffff\1\11\12\uffff\1\12",
            "",
            ""
    };

    static final short[] DFA7_eot = DFA.unpackEncodedString(DFA7_eotS);
    static final short[] DFA7_eof = DFA.unpackEncodedString(DFA7_eofS);
    static final char[] DFA7_min = DFA.unpackEncodedStringToUnsignedChars(DFA7_minS);
    static final char[] DFA7_max = DFA.unpackEncodedStringToUnsignedChars(DFA7_maxS);
    static final short[] DFA7_accept = DFA.unpackEncodedString(DFA7_acceptS);
    static final short[] DFA7_special = DFA.unpackEncodedString(DFA7_specialS);
    static final short[][] DFA7_transition;

    static {
        int numStates = DFA7_transitionS.length;
        DFA7_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA7_transition[i] = DFA.unpackEncodedString(DFA7_transitionS[i]);
        }
    }

    class DFA7 extends DFA {

        public DFA7(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 7;
            this.eot = DFA7_eot;
            this.eof = DFA7_eof;
            this.min = DFA7_min;
            this.max = DFA7_max;
            this.accept = DFA7_accept;
            this.special = DFA7_special;
            this.transition = DFA7_transition;
        }
        public String getDescription() {
            return "174:1: conditions : ( condition AND condition ( AND condition )* | condition );";
        }
    }
 

    public static final BitSet FOLLOW_where_in_query177 = new BitSet(new long[]{0x0000000000040022L});
    public static final BitSet FOLLOW_orderBy_in_query180 = new BitSet(new long[]{0x0000000000040002L});
    public static final BitSet FOLLOW_limit_in_query183 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_WHERE_in_where197 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_conditions_in_where200 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ORDER_in_orderBy213 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_BY_in_orderBy216 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_fieldSort_in_orderBy219 = new BitSet(new long[]{0x0000000000008002L});
    public static final BitSet FOLLOW_COMMA_in_orderBy222 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_fieldSort_in_orderBy225 = new BitSet(new long[]{0x0000000000008002L});
    public static final BitSet FOLLOW_field_in_fieldSort240 = new BitSet(new long[]{0x0000000000030000L});
    public static final BitSet FOLLOW_sortDirection_in_fieldSort242 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_field_in_fieldSort249 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_sortDirection0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LIMIT_in_limit289 = new BitSet(new long[]{0x0000000000084000L});
    public static final BitSet FOLLOW_limitArg_in_limit292 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_limitArg0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_condition_in_conditions324 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_AND_in_conditions326 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_condition_in_conditions329 = new BitSet(new long[]{0x0000000000000082L});
    public static final BitSet FOLLOW_AND_in_conditions332 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_condition_in_conditions335 = new BitSet(new long[]{0x0000000000000082L});
    public static final BitSet FOLLOW_condition_in_conditions343 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_field_in_condition356 = new BitSet(new long[]{0x0000000000001F00L});
    public static final BitSet FOLLOW_compare_op_in_condition358 = new BitSet(new long[]{0x0000000000784000L});
    public static final BitSet FOLLOW_conditionValue_in_condition361 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_compare_op0 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_qualifiedFieldName_in_field407 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_qualifiedFieldName428 = new BitSet(new long[]{0x0000000000800002L});
    public static final BitSet FOLLOW_DOT_in_qualifiedFieldName431 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_ID_in_qualifiedFieldName434 = new BitSet(new long[]{0x0000000000800002L});
    public static final BitSet FOLLOW_PLACEHOLDER_in_conditionValue449 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_CONSTANT_INTEGER_in_conditionValue455 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_constantBoolean_in_conditionValue461 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_CONSTANT_STRING_in_conditionValue467 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_constantBoolean0 = new BitSet(new long[]{0x0000000000000002L});

}