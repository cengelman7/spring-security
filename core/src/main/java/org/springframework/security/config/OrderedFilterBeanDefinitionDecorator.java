package org.springframework.security.config;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Ordered;
import org.springframework.security.ui.FilterChainOrder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Replaces a Spring bean of type "Filter" with a wrapper class which implements the <tt>Ordered</tt>
 * interface. This allows user to add their own filter to the security chain. If the user's filter
 * already implements Ordered, and no "order" attribute is specified, the filter's default order will be used.
 *
 * @author Luke Taylor
 * @version $Id$
 */
public class OrderedFilterBeanDefinitionDecorator implements BeanDefinitionDecorator {

    public static final String ATT_AFTER = "after";
    public static final String ATT_BEFORE = "before";
    public static final String ATT_POSITION = "position";    

    public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder holder, ParserContext parserContext) {
        Element elt = (Element)node;
        String order = getOrder(elt, parserContext);

        BeanDefinition filter = holder.getBeanDefinition();
        BeanDefinitionBuilder wrapper = BeanDefinitionBuilder.rootBeanDefinition("org.springframework.security.config.OrderedFilterBeanDefinitionDecorator$OrderedFilterDecorator");
        wrapper.addConstructorArg(holder.getBeanName());
        wrapper.addConstructorArg(filter);

        if (StringUtils.hasText(order)) {
            wrapper.addPropertyValue("order", order);
        }

        return new BeanDefinitionHolder(wrapper.getBeanDefinition(), holder.getBeanName());
    }

    /**
     * Attempts to get the order of the filter by parsing the 'before' or 'after' attributes.
     */
    private String getOrder(Element elt, ParserContext pc) {
        String after = elt.getAttribute(ATT_AFTER);
        String before = elt.getAttribute(ATT_BEFORE);
        String position = elt.getAttribute(ATT_POSITION);
        
        if(ConfigUtils.countNonEmpty(new String[] {after, before, position}) != 1) {
        	pc.getReaderContext().error("A single '" + ATT_AFTER + "', '" + ATT_BEFORE + "', or '" +
        			ATT_POSITION + "' attribute must be supplied", pc.extractSource(elt));
        }
        
        if (StringUtils.hasText(position)) {
        	return Integer.toString(FilterChainOrder.getOrder(position));
        }
        
        if (StringUtils.hasText(after)) {
            return Integer.toString(FilterChainOrder.getOrder(after) + 1);
        }

        if (StringUtils.hasText(before)) {
            return Integer.toString(FilterChainOrder.getOrder(before) - 1);
        }

        return null;
    }

    static class OrderedFilterDecorator implements Filter, Ordered {
        private Integer order = null;
        private Filter delegate;
        private String beanName;

        OrderedFilterDecorator(String beanName, Filter delegate) {
            this.delegate = delegate;
            this.beanName = beanName;
        }

        public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            delegate.doFilter(request, response, chain);
        }

        public final void init(FilterConfig filterConfig) throws ServletException {
            delegate.init(filterConfig);
        }

        public final void destroy() {
            delegate.destroy();
        }

        public final int getOrder() {
            if(order == null) {
                Assert.isInstanceOf(Ordered.class, delegate, "Filter '"+ beanName +"' must implement the 'Ordered' interface " +
                        " or you must specify one of the attributes '" + ATT_AFTER + "' or '" +
                        ATT_BEFORE + "' in <" + Elements.CUSTOM_FILTER +">");

                return ((Ordered)delegate).getOrder();
            }
            return order.intValue();
        }

        public final void setOrder(int order) {
            this.order = new Integer(order);
        }

        public String getBeanName() {
            return beanName;
        }

		public String toString() {
			return getClass() + "[ delegate=" + delegate + "; order=" + getOrder() + "]";
		}
    }
}